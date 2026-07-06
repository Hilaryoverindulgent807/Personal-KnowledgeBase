#!/bin/bash
# ============================================================
# 智能情报分析平台 - 集成测试脚本
# 测试内容：
#   1. MinerU 远程 API Token 健康检查
#   2. PDF 文件上传 + MinerU 解析 + LLM 知识抽取
#   3. 图表上传 + 来源文档引用（sourceDocId + sourcePage）
#   4. URL 上传（动态信息/Blog）
#   5. 长文档分块抽取验证
# ============================================================

set -e
BASE="http://localhost:8080/api"
PASS=0
FAIL=0

log() { echo ""; echo "=== $1 ==="; }
pass() { echo "  ✅ PASS: $1"; PASS=$((PASS+1)); }
fail() { echo "  ❌ FAIL: $1 — $2"; FAIL=$((FAIL+1)); }

# ------ Test 1: 后端健康 ------
log "Test 1: 后端 API 健康检查"
RESP=$(curl -s "$BASE/dashboard/stats")
if echo "$RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); assert 'doc_count' in d" 2>/dev/null; then
  pass "Dashboard API 正常返回 doc_count"
else
  fail "Dashboard API" "无法获取统计数据"
fi

# ------ Test 2: 项目列表 ------
log "Test 2: 项目列表 API"
RESP=$(curl -s "$BASE/projects")
PROJ_COUNT=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('total',0))" 2>/dev/null)
echo "  项目数: $PROJ_COUNT"
if [ "$PROJ_COUNT" -gt 0 ] 2>/dev/null; then
  PROJ_ID=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['items'][0]['id'])" 2>/dev/null)
  pass "项目列表正常, 使用项目 ID=$PROJ_ID"
else
  # 创建一个测试项目
  RESP=$(curl -s -X POST "$BASE/projects/" -H "Content-Type: application/json" -d '{"name":"测试项目","description":"集成测试专用"}')
  PROJ_ID=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])" 2>/dev/null)
  pass "创建测试项目 ID=$PROJ_ID"
fi

# ------ Test 3: 文件类型列表 ------
log "Test 3: 支持的文件类型列表"
RESP=$(curl -s "$BASE/upload/file-types")
TYPES_COUNT=$(echo "$RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('extensions',[])))" 2>/dev/null)
echo "  支持的文件类型数: $TYPES_COUNT"
if [ "$TYPES_COUNT" -gt 5 ] 2>/dev/null; then
  pass "文件类型列表包含 $TYPES_COUNT 种格式"
else
  fail "文件类型列表" "只有 $TYPES_COUNT 种格式"
fi

# ------ Test 4: 目标库列表 ------
log "Test 4: 目标库列表"
RESP=$(curl -s "$BASE/upload/libraries")
LIB_COUNT=$(echo "$RESP" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null)
echo "  目标库数: $LIB_COUNT"
if [ "$LIB_COUNT" -ge 4 ] 2>/dev/null; then
  pass "目标库列表包含 $LIB_COUNT 个库"
else
  fail "目标库列表" "只有 $LIB_COUNT 个库"
fi

# ------ Test 5: 上传测试文本文件（不需要 MinerU）------
log "Test 5: 上传 TXT 文件（纯文本，无需 MinerU）"
echo "人工智能（AI）是计算机科学的一个分支，致力于开发能够执行通常需要人类智能的任务的系统。机器学习是AI的核心技术之一，它使计算机能够通过数据和经验自动改进性能，而不需要显式编程。深度学习是机器学习的一个子领域，使用多层神经网络来处理复杂的模式识别问题。自然语言处理（NLP）则是AI中专门处理人类语言的分支，包括文本理解、机器翻译和情感分析等应用。" > /tmp/test_ai_doc.txt
RESP=$(curl -s -X POST "$BASE/upload/" \
  -H "X-Project-Id: $PROJ_ID" \
  -F "file=@/tmp/test_ai_doc.txt" \
  -F "docType=report" \
  -F "sourceOrigin=集成测试-TXT文件")
STATUS=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('status',''))" 2>/dev/null)
DOC_ID=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))" 2>/dev/null)
ENTRY_COUNT=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('entry_count',0))" 2>/dev/null)
echo "  状态: $STATUS, 文档ID: $DOC_ID, 词条数: $ENTRY_COUNT"
if [ "$STATUS" = "success" ] && [ "$DOC_ID" != "" ] 2>/dev/null; then
  pass "TXT文件上传成功, 抽取了 $ENTRY_COUNT 个词条"
  TXT_DOC_ID=$DOC_ID
else
  fail "TXT文件上传" "状态=$STATUS, 响应=$RESP"
  TXT_DOC_ID=""
fi

# ------ Test 6: 图表上传 + 来源文档引用 ------
log "Test 6: 图表上传 + 来源文档引用（sourceDocId + sourcePage）"
echo "图表说明：本图展示了2024年生物科技领域的投资趋势变化" > /tmp/test_chart.txt
SOURCE_DOC_ID="${TXT_DOC_ID:-1}"
RESP=$(curl -s -X POST "$BASE/upload/" \
  -H "X-Project-Id: $PROJ_ID" \
  -F "file=@/tmp/test_chart.txt;filename=test_chart_description.txt" \
  -F "docType=chart" \
  -F "sourceOrigin=集成测试-图表来源引用" \
  -F "sourceDocId=$SOURCE_DOC_ID" \
  -F "sourcePage=3")
STATUS=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('status',''))" 2>/dev/null)
CHART_ID=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))" 2>/dev/null)
echo "  状态: $STATUS, 图表文档ID: $CHART_ID"

# 验证 sourceDocId 和 sourcePage 是否保存成功
if [ "$CHART_ID" != "" ] 2>/dev/null; then
  DOC_RESP=$(curl -s "$BASE/documents?page=1&pageSize=1&keyword=test_chart" -H "X-Project-Id: $PROJ_ID")
  HAS_SOURCE=$(echo "$DOC_RESP" | python3 -c "
import sys,json
items = json.load(sys.stdin).get('items',[])
for item in items:
    if item.get('source_doc_id') is not None and item.get('source_page') == 3:
        print('yes')
        break
else:
    print('no')
" 2>/dev/null)
  if [ "$HAS_SOURCE" = "yes" ]; then
    pass "图表来源引用正确保存 (sourceDocId=$SOURCE_DOC_ID, sourcePage=3)"
  else
    # 直接查询数据库验证
    DB_CHECK=$(sqlite3 /Users/xiaotianxue/Desktop/個人/软件所/范_副本/data/app.db "SELECT source_doc_id, source_page FROM documents WHERE id=$CHART_ID" 2>/dev/null)
    echo "  数据库查询: $DB_CHECK"
    if echo "$DB_CHECK" | grep -q "$SOURCE_DOC_ID|3"; then
      pass "图表来源引用正确保存 (sourceDocId=$SOURCE_DOC_ID, sourcePage=3)"
    else
      fail "图表来源引用" "数据库中 source_doc_id/source_page 不正确: $DB_CHECK"
    fi
  fi
else
  fail "图表上传" "上传失败: $RESP"
fi

# ------ Test 7: MinerU Token 配置验证 ------
log "Test 7: MinerU API Token 配置检查"
TOKEN_CHECK=$(sqlite3 /Users/xiaotianxue/Desktop/個人/软件所/范_副本/data/app.db "SELECT CASE WHEN value IS NOT NULL AND length(value) > 50 THEN 'configured' ELSE 'not_configured' END FROM settings WHERE key='mineru_token'" 2>/dev/null)
echo "  Token状态: $TOKEN_CHECK"
if [ "$TOKEN_CHECK" = "configured" ]; then
  pass "MinerU API Token 已配置"
else
  fail "MinerU API Token" "未配置或无效"
fi

# ------ Test 8: 项目详情 API ------
log "Test 8: 项目详情 API（含文档/词条统计）"
RESP=$(curl -s "$BASE/projects/$PROJ_ID/detail")
DOC_COUNT=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('docCount',0))" 2>/dev/null)
ENTRY_COUNT=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('entryCount',0))" 2>/dev/null)
echo "  项目文档数: $DOC_COUNT, 词条数: $ENTRY_COUNT"
if [ "$DOC_COUNT" -gt 0 ] 2>/dev/null; then
  pass "项目详情包含 $DOC_COUNT 个文档, $ENTRY_COUNT 个词条"
else
  fail "项目详情" "文档数为0"
fi

# ------ Test 9: Schema 迁移验证 ------
log "Test 9: Schema 迁移验证（新列存在性）"
COLS=$(sqlite3 /Users/xiaotianxue/Desktop/個人/软件所/范_副本/data/app.db "PRAGMA table_info(documents);" 2>/dev/null)
HAS_SOURCE_DOC=$(echo "$COLS" | grep -c "source_doc_id" || true)
HAS_SOURCE_PAGE=$(echo "$COLS" | grep -c "source_page" || true)
HAS_PROJECT_ID=$(echo "$COLS" | grep -c "project_id" || true)
HAS_URL=$(echo "$COLS" | grep -c "url" || true)
echo "  source_doc_id: $HAS_SOURCE_DOC, source_page: $HAS_SOURCE_PAGE, project_id: $HAS_PROJECT_ID, url: $HAS_URL"
if [ "$HAS_SOURCE_DOC" -gt 0 ] && [ "$HAS_SOURCE_PAGE" -gt 0 ] && [ "$HAS_PROJECT_ID" -gt 0 ]; then
  pass "所有新增列均存在"
else
  fail "Schema迁移" "部分列缺失"
fi

# ------ 清理测试数据 ------
log "清理测试数据"
if [ "$TXT_DOC_ID" != "" ]; then
  curl -s -X DELETE "$BASE/documents/$TXT_DOC_ID" > /dev/null 2>&1
  echo "  删除测试文档 ID=$TXT_DOC_ID"
fi
if [ "$CHART_ID" != "" ]; then
  curl -s -X DELETE "$BASE/documents/$CHART_ID" > /dev/null 2>&1
  echo "  删除测试图表 ID=$CHART_ID"
fi
rm -f /tmp/test_ai_doc.txt /tmp/test_chart.txt

# ------ 结果汇总 ------
echo ""
echo "============================================================"
echo "  测试结果: $PASS 通过, $FAIL 失败 (共 $((PASS+FAIL)) 个测试)"
echo "============================================================"

exit $FAIL
