#!/bin/bash
# ============================================================
# 图片和表格功能集成测试
# 测试内容：
#   1. 图片上传 → 图表库（mediaType=image）
#   2. 表格上传 → 图表库（mediaType=table, Markdown格式）
#   3. 图片和表格的 embedding（向量索引）
#   4. QA 回答返回图片和表格
#   5. 超时修复验证
# ============================================================

set -e
BASE="http://localhost:8080/api"
PASS=0
FAIL=0

log() { echo ""; echo "=== $1 ==="; }
pass() { echo "  ✅ PASS: $1"; PASS=$((PASS+1)); }
fail() { echo "  ❌ FAIL: $1 — $2"; FAIL=$((FAIL+1)); }

# 创建测试数据目录
TEST_DIR="/tmp/test_image_table_$$"
mkdir -p "$TEST_DIR"

# ------ Test 1: 创建测试图片文件 ------
log "Test 1: 创建测试图片文件"
echo "这是一个测试图片的描述文本，用于VLM生成描述" > "$TEST_DIR/test_image.jpg"
echo "Created test image at $TEST_DIR/test_image.jpg"
pass "测试图片文件创建成功"

# ------ Test 2: 创建测试表格文件（CSV）------
log "Test 2: 创建测试表格文件（CSV）"
cat > "$TEST_DIR/test_table.csv" << 'EOF'
指标名称,数值,单位,同比变化
社区医院数量,35,万家,5.2%
诊疗人次,22,亿次,-8.3%
医保控费比例,15.7%,-,3.2pp
老龄化率,21.1%,-,1.5pp
慢病患者,3.2,亿人,4.1%
EOF
echo "Created test CSV at $TEST_DIR/test_table.csv"
pass "测试表格文件创建成功"

# ------ Test 3: 上传图片到图表库 ------
log "Test 3: 上传图片到图表库"
RESP=$(curl -s -X POST "$BASE/upload/" \
  -H "X-Project-Id: 11" \
  -F "file=@$TEST_DIR/test_image.jpg" \
  -F "docType=chart" \
  -F "sourceOrigin=测试图片")
STATUS=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('status',''))" 2>/dev/null)
IMG_ID=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))" 2>/dev/null)
echo "  状态: $STATUS, 文档ID: $IMG_ID"
if [ "$STATUS" = "success" ] && [ "$IMG_ID" != "" ]; then
  pass "图片上传成功 (docId=$IMG_ID)"
else
  fail "图片上传" "状态=$STATUS, 响应=$RESP"
fi

# ------ Test 4: 上传表格到图表库 ------
log "Test 4: 上传表格到图表库（CSV）"
RESP=$(curl -s -X POST "$BASE/upload/" \
  -H "X-Project-Id: 11" \
  -F "file=@$TEST_DIR/test_table.csv" \
  -F "docType=chart" \
  -F "sourceOrigin=测试表格")
STATUS=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('status',''))" 2>/dev/null)
TBL_ID=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))" 2>/dev/null)
echo "  状态: $STATUS, 文档ID: $TBL_ID"
if [ "$STATUS" = "success" ] && [ "$TBL_ID" != "" ]; then
  pass "表格上传成功 (docId=$TBL_ID)"
else
  fail "表格上传" "状态=$STATUS, 响应=$RESP"
fi

# ------ Test 5: 验证图片和表格的 mediaType ------
log "Test 5: 验证图片和表格的 mediaType 正确性"
sleep 2  # 等待异步处理完成
IMG_MEDIA=$(sqlite3 /Users/xiaotianxue/Desktop/個人/软件所/范_副本/data/app.db "SELECT media_type FROM knowledge_entries WHERE document_id=$IMG_ID LIMIT 1;" 2>/dev/null)
TBL_MEDIA=$(sqlite3 /Users/xiaotianxue/Desktop/個人/软件所/范_副本/data/app.db "SELECT media_type FROM knowledge_entries WHERE document_id=$TBL_ID LIMIT 1;" 2>/dev/null)
echo "  图片 mediaType: $IMG_MEDIA"
echo "  表格 mediaType: $TBL_MEDIA"
if [ "$IMG_MEDIA" = "image" ]; then
  pass "图片 mediaType 正确 (image)"
else
  fail "图片 mediaType" "期望=image, 实际=$IMG_MEDIA"
fi
if [ "$TBL_MEDIA" = "table" ]; then
  pass "表格 mediaType 正确 (table)"
else
  fail "表格 mediaType" "期望=table, 实际=$TBL_MEDIA"
fi

# ------ Test 6: 验证表格 Markdown 格式 ------
log "Test 6: 验证表格 Markdown 格式存储"
TBL_MD=$(sqlite3 /Users/xiaotianxue/Desktop/個人/软件所/范_副本/data/app.db "SELECT table_markdown FROM knowledge_entries WHERE document_id=$TBL_ID LIMIT 1;" 2>/dev/null)
if echo "$TBL_MD" | grep -q "|"; then
  echo "  表格 Markdown 格式: $(echo "$TBL_MD" | head -c 100)..."
  pass "表格以 Markdown 格式存储"
else
  fail "表格 Markdown" "未找到有效的 Markdown 格式"
fi

# ------ Test 7: 验证 embedding（向量索引）------
log "Test 7: 验证图片和表格的 embedding（向量索引）"
# 调用向量搜索 API 测试图片和表格是否被索引
SEARCH_RESP=$(curl -s "$BASE/vector-search/test" -H "X-Project-Id: 11" 2>/dev/null)
if echo "$SEARCH_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); assert d.get('status')=='ok'" 2>/dev/null; then
  pass "向量搜索服务正常"
else
  # 直接查询数据库验证
  IMG_COUNT=$(sqlite3 /Users/xiaotianxue/Desktop/個人/软件所/范_副本/data/app.db "SELECT COUNT(*) FROM knowledge_entries WHERE document_id=$IMG_ID AND media_type='image';" 2>/dev/null)
  TBL_COUNT=$(sqlite3 /Users/xiaotianxue/Desktop/個人/软件所/范_副本/data/app.db "SELECT COUNT(*) FROM knowledge_entries WHERE document_id=$TBL_ID AND media_type='table';" 2>/dev/null)
  if [ "$IMG_COUNT" -gt 0 ] && [ "$TBL_COUNT" -gt 0 ]; then
    pass "图片和表格已创建知识词条（可被 embedding）"
  else
    fail "Embedding" "图片=$IMG_COUNT, 表格=$TBL_COUNT"
  fi
fi

# ------ Test 8: 验证超时修复（检查前端配置）------
log "Test 8: 验证超时修复（5分钟超时）"
TIMEOUT_CHECK=$(grep -A 2 "askQuestion" /Users/xiaotianxue/Desktop/個人/软件所/范_副本/frontend-vue/src/api/index.ts | grep "timeout: 300000" || echo "")
if [ -n "$TIMEOUT_CHECK" ]; then
  pass "QA API 超时已修复为 300000ms（5分钟）"
else
  fail "超时修复" "未找到 300000ms 超时配置"
fi

# ------ Test 9: 验证 QA 回答结构化返回（tables/images）------
log "Test 9: 验证 QA 回答支持结构化返回（tables/images）"
# 检查后端 Controller 是否有 tables 和 images 字段
QA_CONTROLLER="/Users/xiaotianxue/Desktop/個人/软件所/范_副本/backend-springboot/src/main/java/com/intelligence/platform/controller/QAChatController.java"
if grep -q "tables" "$QA_CONTROLLER" && grep -q "images" "$QA_CONTROLLER"; then
  pass "QAChatController 支持 tables/images 返回"
else
  fail "QA 结构化返回" "Controller 未找到 tables/images 字段"
fi

# ------ Test 10: 验证前端 SmartQA 组件渲染图片和表格 ------
log "Test 10: 验证前端 SmartQA 组件渲染图片和表格"
SMARTQA="/Users/xiaotianxue/Desktop/個人/软件所/范_副本/frontend-vue/src/views/portal/SmartQA.vue"
if grep -q "tables" "$SMARTQA" && grep -q "images" "$SMARTQA"; then
  pass "SmartQA.vue 支持渲染 tables/images"
else
  fail "SmartQA 渲染" "组件未找到 tables/images 处理"
fi

# ------ 清理测试数据 ------
log "清理测试数据"
if [ "$IMG_ID" != "" ]; then
  curl -s -X DELETE "$BASE/documents/$IMG_ID" > /dev/null 2>&1
  echo "  删除测试图片 ID=$IMG_ID"
fi
if [ "$TBL_ID" != "" ]; then
  curl -s -X DELETE "$BASE/documents/$TBL_ID" > /dev/null 2>&1
  echo "  删除测试表格 ID=$TBL_ID"
fi
rm -rf "$TEST_DIR"

# ------ 结果汇总 ------
echo ""
echo "============================================================"
echo "  测试结果: $PASS 通过, $FAIL 失败 (共 $((PASS+FAIL)) 个测试)"
echo "============================================================"

exit $FAIL
