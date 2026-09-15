#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

SRC_DIR="/storage/emulated/0/黑白君/课程表/最新"
BRANCH="master"
EXPECTED_APPLICATION_ID="com.hufeng943.timetable"
EXPECTED_WEARABLE_VERSION="20.0.1"

die() {
    echo
    echo "❌ $*"
    exit 1
}

ok() {
    echo "✅ $*"
}

echo
echo "========================================"
echo " Timetable 3.4.x 安全一键更新"
echo " GitHub Secrets Release Signing"
echo "========================================"
echo

# ------------------------------------------------------------
# 1. 基础检查
# ------------------------------------------------------------

[ -d ".git" ] || die "当前目录不是 Git 仓库"

git remote get-url origin >/dev/null 2>&1 || die "找不到 Git remote: origin"

[ -d "$SRC_DIR" ] || die "新源码目录不存在：$SRC_DIR"

ok "Git 仓库正常：$(git remote get-url origin)"
ok "新源码目录正常：$SRC_DIR"

# ------------------------------------------------------------
# 2. 关键文件
# ------------------------------------------------------------

REQUIRED_FILES=(
    "settings.gradle.kts"
    "gradle/libs.versions.toml"
    "mobile/build.gradle.kts"
    "wear/build.gradle.kts"
    "gradle.properties"
    ".gitignore"
    ".github/workflows/release.yml"
    "SIGNING.md"
)

for file in "${REQUIRED_FILES[@]}"; do
    [ -f "$SRC_DIR/$file" ] || die "新源码缺少关键文件：$file"
done

ok "新源码关键文件完整"

# ------------------------------------------------------------
# 3. 源码绝不能包含 JKS / keystore
# ------------------------------------------------------------

echo
echo "🔐 检查新源码私钥..."

KEY_FILES="$(find "$SRC_DIR" -type f | grep -Ei '\.(jks|keystore|p12|pfx)$' || true)"

if [ -n "$KEY_FILES" ]; then
    echo "$KEY_FILES"
    die "新源码包含私钥文件，禁止继续"
fi

ok "新源码没有 JKS/keystore"

# ------------------------------------------------------------
# 4. .gitignore
# ------------------------------------------------------------

grep -Fq '*.jks' "$SRC_DIR/.gitignore" || die ".gitignore 缺少 *.jks"
grep -Fq '*.keystore' "$SRC_DIR/.gitignore" || die ".gitignore 缺少 *.keystore"

ok ".gitignore 私钥保护正常"

# ------------------------------------------------------------
# 5. applicationId
# ------------------------------------------------------------

echo
echo "📱 检查 applicationId..."

grep -Eq "applicationId[[:space:]]*=[[:space:]]*\"$EXPECTED_APPLICATION_ID\"" "$SRC_DIR/mobile/build.gradle.kts" || die "Mobile applicationId 不正确"

ok "applicationId = $EXPECTED_APPLICATION_ID"

# ------------------------------------------------------------
# 6. Wear versionCode
# ------------------------------------------------------------

echo
echo "⌚ 检查 Wear versionCode..."

if grep -Eq 'versionCode[[:space:]]*=[[:space:]]*[0-9]+if[[:space:]]*\(' "$SRC_DIR/wear/build.gradle.kts"; then
    die "检测到损坏的 Wear versionCode"
fi

grep -Fq 'versionCode = if (isRelease) commitCountProvider.get() else 1' "$SRC_DIR/wear/build.gradle.kts" || die "Wear versionCode 表达式异常"

ok "Wear versionCode 正常"

# ------------------------------------------------------------
# 7. Wearable dependency
# ------------------------------------------------------------

echo
echo "🔗 检查 Wearable dependency..."

grep -Fq "playServicesWearable = \"$EXPECTED_WEARABLE_VERSION\"" "$SRC_DIR/gradle/libs.versions.toml" || die "play-services-wearable 版本异常"

ok "play-services-wearable = $EXPECTED_WEARABLE_VERSION"

# ------------------------------------------------------------
# 8. Release signing 配置
# ------------------------------------------------------------

echo
echo "🖊️ 检查 Secrets signing..."

SIGNING_VARS=(
    "TIMETABLE_RELEASE_STORE_FILE"
    "TIMETABLE_RELEASE_STORE_PASSWORD"
    "TIMETABLE_RELEASE_KEY_ALIAS"
    "TIMETABLE_RELEASE_KEY_PASSWORD"
)

for gradle_file in "$SRC_DIR/mobile/build.gradle.kts" "$SRC_DIR/wear/build.gradle.kts"; do
    for var in "${SIGNING_VARS[@]}"; do
        grep -Fq "$var" "$gradle_file" || die "$(basename "$gradle_file") 缺少 $var"
    done

    grep -Fq 'GradleException' "$gradle_file" || die "$(basename "$gradle_file") 缺少 Release signing fail-fast"
done

ok "Mobile/Wear Secrets signing 正常"

# ------------------------------------------------------------
# 9. GitHub Actions
# ------------------------------------------------------------

echo
echo "⚙️ 检查 GitHub Actions..."

WORKFLOW="$SRC_DIR/.github/workflows/release.yml"

WORKFLOW_SECRETS=(
    "TIMETABLE_RELEASE_JKS_BASE64"
    "TIMETABLE_RELEASE_STORE_PASSWORD"
    "TIMETABLE_RELEASE_KEY_ALIAS"
    "TIMETABLE_RELEASE_KEY_PASSWORD"
)

for var in "${WORKFLOW_SECRETS[@]}"; do
    grep -Fq "$var" "$WORKFLOW" || die "release.yml 缺少 $var"
done

grep -Fq ':mobile:assembleRelease' "$WORKFLOW" || die "release.yml 缺少 Mobile Release"
grep -Fq ':wear:assembleRelease' "$WORKFLOW" || die "release.yml 缺少 Wear Release"
grep -Fq 'keytool' "$WORKFLOW" || die "release.yml 缺少 keytool 校验"
grep -Fq 'base64' "$WORKFLOW" || die "release.yml 缺少 JKS Base64 恢复"

ok "GitHub Actions Release 流程正常"

# ------------------------------------------------------------
# 10. AAPT2
# ------------------------------------------------------------

echo
echo "🧱 检查 AAPT2..."

if grep -Eq 'android\.aapt2Override|android\.aapt2FromMaven' "$SRC_DIR/gradle.properties"; then
    die "仍存在 AAPT2 强制覆盖"
fi

ok "AAPT2 配置正常"

# ------------------------------------------------------------
# 11. 已知 P0
# ------------------------------------------------------------

echo
echo "🔎 检查已知 P0 残留..."

if grep -R --include='*.kt' -n 'scheduleTools(' "$SRC_DIR/mobile" "$SRC_DIR/wear" 2>/dev/null; then
    die "发现 scheduleTools() 残留"
fi

if grep -R --include='*.kt' -n 'MORE_ABOUT_DEVELOPER_PROBE' "$SRC_DIR/mobile" "$SRC_DIR/wear" 2>/dev/null; then
    die "发现 MORE_ABOUT_DEVELOPER_PROBE 残留"
fi

ok "已知 P0 残留检查通过"

# ------------------------------------------------------------
# 12. 检查 Markdown 污染
# ------------------------------------------------------------

echo
echo "🧹 检查源码是否被 Markdown 代码围栏污染..."

BAD_GRADLE_FENCES="$(grep -R -n '```' "$SRC_DIR" --include='*.kt' --include='*.kts' --include='*.toml' --include='*.properties' 2>/dev/null || true)"

if [ -n "$BAD_GRADLE_FENCES" ]; then
    echo "$BAD_GRADLE_FENCES"
    die "源码文件发现 Markdown 代码围栏污染"
fi

ok "未发现 Markdown 围栏污染"

# ------------------------------------------------------------
# 13. 同步远端
# ------------------------------------------------------------

echo
echo "========================================"
echo " 同步 GitHub $BRANCH"
echo "========================================"
echo

git fetch origin "$BRANCH"
git checkout -B "$BRANCH" "origin/$BRANCH"
git reset --hard "origin/$BRANCH"

ok "已同步 origin/$BRANCH"

# ------------------------------------------------------------
# 14. 清理旧工作树
# ------------------------------------------------------------

echo
echo "🧹 清理旧源码..."

find . -mindepth 1 -maxdepth 1 ! -name '.git' ! -name 'update_timetable.sh' -exec rm -rf {} +

ok "旧源码已清理"

# ------------------------------------------------------------
# 15. 复制新源码
# ------------------------------------------------------------

echo
echo "📦 复制新源码..."

cp -a "$SRC_DIR"/. ./

ok "新源码复制完成"

# ------------------------------------------------------------
# 16. 复制后二次检查
# ------------------------------------------------------------

echo
echo "🔍 复制后二次安全检查..."

for file in "${REQUIRED_FILES[@]}"; do
    [ -f "$file" ] || die "复制后缺少：$file"
done

grep -Eq "applicationId[[:space:]]*=[[:space:]]*\"$EXPECTED_APPLICATION_ID\"" mobile/build.gradle.kts || die "复制后 applicationId 异常"

grep -Fq 'versionCode = if (isRelease) commitCountProvider.get() else 1' wear/build.gradle.kts || die "复制后 Wear versionCode 异常"

grep -Fq "playServicesWearable = \"$EXPECTED_WEARABLE_VERSION\"" gradle/libs.versions.toml || die "复制后 Wearable dependency 异常"

COPIED_KEYS="$(find . -type f ! -path './.git/*' | grep -Ei '\.(jks|keystore|p12|pfx)$' || true)"

if [ -n "$COPIED_KEYS" ]; then
    echo "$COPIED_KEYS"
    die "复制后工作树出现私钥"
fi

ok "复制后二次检查通过"

# ------------------------------------------------------------
# 17. Git 索引检查
# ------------------------------------------------------------

echo
echo "🛡️ 检查 Git 私钥..."

TRACKED_KEYS="$(git ls-files | grep -Ei '\.(jks|keystore|p12|pfx)$' || true)"

if [ -n "$TRACKED_KEYS" ]; then
    echo "$TRACKED_KEYS"
    die "Git 当前仍跟踪私钥文件"
fi

ok "Git 当前没有跟踪私钥"

# ------------------------------------------------------------
# 18. 获取版本
# ------------------------------------------------------------

MOBILE_VERSION="$(grep -oE 'versionName[[:space:]]*=[[:space:]]*"[^"]+"' mobile/build.gradle.kts | head -1 | cut -d'"' -f2 || true)"

echo
echo "========================================"
echo " 当前版本"
echo "========================================"
echo
echo "📱 Mobile versionName: ${MOBILE_VERSION:-动态/未知}"
echo
echo "⌚ Wear:"
grep -nE 'versionPrefix|versionCode|versionName' wear/build.gradle.kts || true
echo
echo "🔐 Release Signing:"
echo "   JKS 不进入 Git"
echo "   GitHub Actions 从 Secrets 临时恢复"

# ------------------------------------------------------------
# 19. Git 状态
# ------------------------------------------------------------

echo
echo "========================================"
echo " Git 变更"
echo "========================================"
echo

git status --short

# ------------------------------------------------------------
# 20. 暂存
# ------------------------------------------------------------

git add -A

git reset -- update_timetable.sh >/dev/null 2>&1 || true

STAGED_KEYS="$(git diff --cached --name-only | grep -Ei '\.(jks|keystore|p12|pfx)$' || true)"

if [ -n "$STAGED_KEYS" ]; then
    echo "$STAGED_KEYS"
    die "暂存区发现私钥文件"
fi

# ------------------------------------------------------------
# 21. 提交前最终保护
# ------------------------------------------------------------

if grep -Eq 'versionCode[[:space:]]*=[[:space:]]*[0-9]+if[[:space:]]*\(' wear/build.gradle.kts; then
    die "提交前 Wear versionCode 损坏"
fi

grep -Eq "applicationId[[:space:]]*=[[:space:]]*\"$EXPECTED_APPLICATION_ID\"" mobile/build.gradle.kts || die "提交前 applicationId 异常"

FINAL_KEYS="$(git ls-files | grep -Ei '\.(jks|keystore|p12|pfx)$' || true)"

if [ -n "$FINAL_KEYS" ]; then
    echo "$FINAL_KEYS"
    die "提交前 Git 索引存在私钥"
fi

# ------------------------------------------------------------
# 22. Commit
# ------------------------------------------------------------

echo

if [ -n "$(git diff --cached --name-only)" ]; then
    if [ -n "${MOBILE_VERSION:-}" ]; then
        COMMIT_MSG="update: Timetable ${MOBILE_VERSION} full source fix"
    else
        COMMIT_MSG="update: Timetable full source fix"
    fi

    echo "📝 提交：$COMMIT_MSG"
    git commit -m "$COMMIT_MSG"
else
    echo "⚠️ 源码与 GitHub 一致"
    echo "🚀 创建空提交触发 Actions"

    if [ -n "${MOBILE_VERSION:-}" ]; then
        git commit --allow-empty -m "ci: rebuild Timetable ${MOBILE_VERSION}"
    else
        git commit --allow-empty -m "ci: rebuild Timetable"
    fi
fi

# ------------------------------------------------------------
# 23. Commit 后检查
# ------------------------------------------------------------

echo
echo "========================================"
echo " 最终检查"
echo "========================================"
echo

git log -1 --oneline

echo
echo "📱 applicationId:"
grep -n 'applicationId' mobile/build.gradle.kts || true

echo
echo "⌚ Wear versionCode:"
grep -n 'versionCode' wear/build.gradle.kts || true

FINAL_KEYS="$(git ls-files | grep -Ei '\.(jks|keystore|p12|pfx)$' || true)"

if [ -n "$FINAL_KEYS" ]; then
    echo "$FINAL_KEYS"
    die "最终检查失败：Git 中存在私钥"
fi

ok "最终源码安全检查通过"

# ------------------------------------------------------------
# 24. Push
# ------------------------------------------------------------

echo
echo "⬆️ 推送到 origin/$BRANCH ..."

if git push origin "$BRANCH"; then
    echo
    ok "推送成功"
else
    echo
    echo "⚠️ 第一次推送失败，30 秒后重试..."
    sleep 30

    if git push origin "$BRANCH"; then
        echo
        ok "第二次推送成功"
    else
        die "两次推送均失败，本地提交已保留"
    fi
fi

echo
echo "========================================"
echo " ✅ 全部完成"
echo "========================================"
echo
echo "版本：${MOBILE_VERSION:-未知}"
echo "包名：$EXPECTED_APPLICATION_ID"
echo "签名：GitHub Secrets + 原正式 JKS"
echo
echo "🚀 请查看 GitHub Actions"
echo "测试通过后将继续构建 Mobile/Wear Release APK"
echo

