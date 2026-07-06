"""
智能情报分析平台 - API 集成测试
测试所有后端 API 端点 + 前端页面服务
运行方式: cd backend && uv run pytest ../tests/test_api.py -v
"""
import sys
import os

# 确保 backend 在 path 中
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'backend'))

import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.database import init_db, get_db, DB_PATH

client = TestClient(app)


@pytest.fixture(autouse=True, scope="session")
def setup_db():
    """确保数据库已初始化并含种子数据"""
    init_db()
    # 如果数据库为空，插入种子数据
    db = get_db()
    if db.execute("SELECT COUNT(*) FROM documents").fetchone()[0] == 0:
        from app.seed import seed
        seed()
    db.close()
    yield
    # 清理: 不删除数据库，以便调试查看


# ============================================================
# 仪表盘 API 测试
# ============================================================
class TestDashboard:
    def test_stats(self):
        """仪表盘统计数据"""
        r = client.get("/api/dashboard/stats")
        assert r.status_code == 200
        d = r.json()
        assert d["doc_count"] >= 15
        assert d["report_count"] >= 5
        assert d["qa_count"] >= 4
        assert d["project_count"] >= 5
        print(f"  ✓ 仪表盘统计: docs={d['doc_count']}, reports={d['report_count']}, qa={d['qa_count']}, projects={d['project_count']}")

    def test_latest_documents(self):
        """最新动态信息"""
        r = client.get("/api/dashboard/latest-documents?limit=3")
        assert r.status_code == 200
        items = r.json()
        assert len(items) == 3
        print(f"  ✓ 最新动态: {len(items)} 条")

    def test_active_projects(self):
        """进行中项目"""
        r = client.get("/api/dashboard/active-projects?limit=3")
        assert r.status_code == 200
        items = r.json()
        assert len(items) >= 3
        print(f"  ✓ 进行项目: {len(items)} 个")


# ============================================================
# 资料管理 API 测试
# ============================================================
class TestDocuments:
    def test_list_all(self):
        """列出所有资料"""
        r = client.get("/api/documents/")
        assert r.status_code == 200
        d = r.json()
        assert d["total"] >= 15
        assert len(d["items"]) <= 20
        print(f"  ✓ 资料列表: total={d['total']}")

    def test_filter_by_type(self):
        """按类型筛选"""
        r = client.get("/api/documents/?doc_type=report")
        assert r.status_code == 200
        d = r.json()
        assert d["total"] > 0
        for item in d["items"]:
            assert item["doc_type"] == "report"
        print(f"  ✓ 按类型筛选(report): {d['total']} 条")

    def test_filter_by_category(self):
        """按分类筛选"""
        r = client.get("/api/documents/?category_l1=装备发展")
        assert r.status_code == 200
        d = r.json()
        assert d["total"] > 0
        print(f"  ✓ 按分类筛选(装备发展): {d['total']} 条")

    def test_pagination(self):
        """分页"""
        r = client.get("/api/documents/?page=1&page_size=5")
        assert r.status_code == 200
        d = r.json()
        assert len(d["items"]) == 5
        assert d["page"] == 1
        assert d["page_size"] == 5
        print(f"  ✓ 分页: page={d['page']}, size={d['page_size']}, total={d['total']}")

    def test_get_single(self):
        """获取单个资料"""
        r = client.get("/api/documents/1")
        assert r.status_code == 200
        doc = r.json()
        assert doc["id"] == 1
        assert "title" in doc
        print(f"  ✓ 单个资料: {doc['title']}")

    def test_get_not_found(self):
        """资料不存在"""
        r = client.get("/api/documents/99999")
        assert r.status_code == 404
        print("  ✓ 404 正确返回")

    def test_crud_cycle(self):
        """完整 CRUD 周期"""
        # 创建
        r = client.post("/api/documents/", json={
            "title": "测试资料-自动创建",
            "category_l1": "测试",
            "category_l2": "单元测试",
            "doc_type": "report",
            "keywords": "test",
        })
        assert r.status_code == 200
        doc_id = r.json()["id"]
        print(f"  ✓ 创建: id={doc_id}")

        # 读取
        r = client.get(f"/api/documents/{doc_id}")
        assert r.status_code == 200
        assert r.json()["title"] == "测试资料-自动创建"
        print(f"  ✓ 读取: {r.json()['title']}")

        # 更新
        r = client.put(f"/api/documents/{doc_id}", json={
            "title": "测试资料-已更新",
            "category_l1": "测试",
            "category_l2": "单元测试",
            "doc_type": "report",
            "keywords": "test,updated",
        })
        assert r.status_code == 200
        print(f"  ✓ 更新成功")

        # 删除
        r = client.delete(f"/api/documents/{doc_id}")
        assert r.status_code == 200
        print(f"  ✓ 删除成功")

        # 确认已删除
        r = client.get(f"/api/documents/{doc_id}")
        assert r.status_code == 404
        print(f"  ✓ 确认已删除")


# ============================================================
# 知识图谱 API 测试
# ============================================================
class TestKnowledgeGraph:
    def test_get_graph(self):
        """获取完整图谱"""
        r = client.get("/api/kg/graph")
        assert r.status_code == 200
        d = r.json()
        assert d["stats"]["node_count"] == 25
        assert d["stats"]["edge_count"] == 44
        assert d["stats"]["community_count"] >= 4
        print(f"  ✓ 图谱: {d['stats']['node_count']}节点, {d['stats']['edge_count']}边, {d['stats']['community_count']}社区")

    def test_edge_types(self):
        """边类型分布"""
        r = client.get("/api/kg/graph")
        d = r.json()
        edge_types = d["stats"]["edge_types"]
        assert "direct" in edge_types
        assert "source_overlap" in edge_types
        assert "adamic_adar" in edge_types
        assert "type_affinity" in edge_types
        print(f"  ✓ 边类型: {edge_types}")

    def test_nodes(self):
        """获取节点列表"""
        r = client.get("/api/kg/nodes")
        assert r.status_code == 200
        nodes = r.json()
        assert len(nodes) == 25
        print(f"  ✓ 节点数: {len(nodes)}")

    def test_edges(self):
        """获取边列表"""
        r = client.get("/api/kg/edges")
        assert r.status_code == 200
        edges = r.json()
        assert len(edges) == 44
        print(f"  ✓ 边数: {len(edges)}")

    def test_insights(self):
        """图谱洞察"""
        r = client.get("/api/kg/insights")
        assert r.status_code == 200
        insights = r.json()
        assert isinstance(insights, list)
        # 洞察可能为空(取决于社区分配)，验证结构正确
        for item in insights:
            assert "type" in item
            assert "title" in item
            assert "desc" in item
        print(f"  ✓ 洞察: {len(insights)}条 (结构验证通过)")


# ============================================================
# 结论冲突 + 方向建议 API 测试
# ============================================================
class TestRisks:
    def test_list(self):
        r = client.get("/api/risks/")
        assert r.status_code == 200
        d = r.json()
        assert d["total"] >= 4
        print(f"  ✓ 冲突列表: {d['total']}条")

    def test_stats(self):
        r = client.get("/api/risks/stats")
        assert r.status_code == 200
        d = r.json()
        assert d["total"] >= 4
        assert "critical" in d["by_severity"]
        print(f"  ✓ 冲突统计: {d['by_severity']}")


class TestDecisions:
    def test_list(self):
        r = client.get("/api/decisions/")
        assert r.status_code == 200
        d = r.json()
        assert d["total"] >= 4
        # 应按分数降序
        scores = [i["score"] for i in d["items"]]
        assert scores == sorted(scores, reverse=True)
        print(f"  ✓ 建议列表: {d['total']}条, 已按分数排序")


# ============================================================
# 其他 API 测试
# ============================================================
class TestOtherAPIs:
    def test_reports(self):
        r = client.get("/api/reports/")
        assert r.status_code == 200
        assert r.json()["total"] >= 5
        print(f"  ✓ 报告库: {r.json()['total']}条")

    def test_qa(self):
        r = client.get("/api/qa/")
        assert r.status_code == 200
        assert r.json()["total"] >= 4
        print(f"  ✓ 问答: {r.json()['total']}条")

    def test_analysis(self):
        r = client.get("/api/analysis/")
        assert r.status_code == 200
        assert r.json()["total"] >= 6
        print(f"  ✓ 分析报告: {r.json()['total']}条")

    def test_settings(self):
        r = client.get("/api/settings/")
        assert r.status_code == 200
        settings = r.json()
        assert len(settings) >= 14
        assert "model_name" in settings
        print(f"  ✓ 配置: {len(settings)}项")


# ============================================================
# 前端页面服务测试
# ============================================================
class TestFrontend:
    def test_index(self):
        r = client.get("/")
        assert r.status_code == 200
        assert "智能情报分析平台" in r.text
        print("  ✓ 首页可访问")

    def test_admin(self):
        r = client.get("/admin.html")
        assert r.status_code == 200
        assert "仪表盘" in r.text
        print("  ✓ 后台首页可访问")

    def test_kg_page(self):
        r = client.get("/kg.html")
        assert r.status_code == 200
        assert "知识图谱" in r.text
        print("  ✓ 知识图谱页可访问")

    def test_css(self):
        r = client.get("/css/admin.css")
        assert r.status_code == 200
        print("  ✓ CSS文件可访问")

    def test_js(self):
        r = client.get("/js/app.js")
        assert r.status_code == 200
        print("  ✓ JS文件可访问")
