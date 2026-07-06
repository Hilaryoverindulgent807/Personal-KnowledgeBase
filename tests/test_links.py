"""
测试1: HTML页面内部链接检查
验证所有HTML文件中的 <a href="xxx.html"> 链接指向的文件是否存在
"""
import os
import re
import sys

def test_html_links():
    """检查所有HTML文件的内部链接是否正确"""
    project_dir = os.path.dirname(os.path.abspath(__file__))

    # 获取所有HTML文件
    html_files = [f for f in os.listdir(project_dir) if f.endswith('.html')]
    html_files.sort()

    print(f"=== HTML链接检查测试 ===")
    print(f"项目目录: {project_dir}")
    print(f"发现 {len(html_files)} 个HTML文件:")
    for f in html_files:
        print(f"  - {f}")
    print()

    # 链接提取正则
    href_pattern = re.compile(r'href="([^"#]*?\.html)"')

    total_links = 0
    broken_links = 0
    results = []

    for html_file in html_files:
        filepath = os.path.join(project_dir, html_file)
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()

        links = href_pattern.findall(content)
        unique_links = sorted(set(links))

        for link in unique_links:
            total_links += 1
            target_path = os.path.join(project_dir, link)
            exists = os.path.exists(target_path)
            if not exists:
                broken_links += 1
                results.append(f"  ✗ [{html_file}] -> {link} (文件不存在!)")
            else:
                results.append(f"  ✓ [{html_file}] -> {link}")

    # 输出结果
    print(f"--- 链接检查结果 ---")
    for r in results:
        print(r)

    print(f"\n--- 统计 ---")
    print(f"总链接数: {total_links}")
    print(f"正常链接: {total_links - broken_links}")
    print(f"断裂链接: {broken_links}")

    if broken_links > 0:
        print(f"\n❌ 测试失败: 发现 {broken_links} 个断裂链接!")
        return False
    else:
        print(f"\n✅ 测试通过: 所有 {total_links} 个链接均正确!")
        return True


def test_html_structure():
    """检查所有HTML文件的基本结构"""
    project_dir = os.path.dirname(os.path.abspath(__file__))
    html_files = [f for f in os.listdir(project_dir) if f.endswith('.html')]

    print(f"\n=== HTML结构检查 ===")

    required_elements = {
        '<!DOCTYPE html>': 'DOCTYPE声明',
        '<html': 'html标签',
        '<head>': 'head标签',
        '<body>': 'body标签',
        '</html>': 'html闭合标签',
    }

    all_pass = True

    for html_file in sorted(html_files):
        filepath = os.path.join(project_dir, html_file)
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()

        file_pass = True
        for element, desc in required_elements.items():
            if element not in content:
                print(f"  ✗ [{html_file}] 缺少 {desc} ({element})")
                file_pass = False
                all_pass = False

        if file_pass:
            print(f"  ✓ [{html_file}] 结构完整")

    if all_pass:
        print(f"\n✅ 结构检查通过!")
    else:
        print(f"\n❌ 部分文件结构不完整!")

    return all_pass


def test_title_consistency():
    """检查所有页面标题是否已更新为军事领域"""
    project_dir = os.path.dirname(os.path.abspath(__file__))
    html_files = [f for f in os.listdir(project_dir) if f.endswith('.html')]

    print(f"\n=== 标题一致性检查 ===")

    all_pass = True
    old_titles = ['科研智能辅助平台', 'Research Intelligence Platform']
    new_title = '智能情报分析平台'

    for html_file in sorted(html_files):
        filepath = os.path.join(project_dir, html_file)
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()

        has_old = any(old in content for old in old_titles)
        has_new = new_title in content or 'Intelligence Analysis Platform' in content

        if has_old:
            print(f"  ✗ [{html_file}] 仍包含旧标题!")
            all_pass = False
        elif has_new:
            print(f"  ✓ [{html_file}] 标题已更新")
        else:
            # kg.html 和其他没有侧栏的页面可能不需要标题
            print(f"  ~ [{html_file}] 无平台标题 (可能是独立页面)")

    if all_pass:
        print(f"\n✅ 标题一致性检查通过!")
    else:
        print(f"\n❌ 部分页面仍包含旧标题!")

    return all_pass


def test_admin_sidebar_consistency():
    """检查所有admin页面的侧栏导航是否一致"""
    project_dir = os.path.dirname(os.path.abspath(__file__))
    admin_files = sorted([f for f in os.listdir(project_dir) if f.startswith('admin') and f.endswith('.html')])

    print(f"\n=== 后台侧栏一致性检查 ===")

    # 所有admin页面应该包含的关键导航项
    required_nav = [
        'admin.html',
        'admin-info-dynamic.html',
        'admin-reports.html',
        'admin-translations.html',
        'admin-charts.html',
        'admin-projects.html',
        'admin-org.html',
        'admin-qa.html',
        'admin-analysis.html',
        'admin-risk.html',
        'admin-decision.html',
        'admin-settings.html',
        'admin-kg.html',
    ]

    all_pass = True

    for admin_file in admin_files:
        filepath = os.path.join(project_dir, admin_file)
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()

        missing = [nav for nav in required_nav if nav not in content]
        if missing:
            print(f"  ✗ [{admin_file}] 缺少导航: {', '.join(missing)}")
            all_pass = False
        else:
            print(f"  ✓ [{admin_file}] 侧栏完整 ({len(required_nav)} 个导航项)")

    if all_pass:
        print(f"\n✅ 侧栏一致性检查通过!")
    else:
        print(f"\n❌ 部分admin页面侧栏不完整!")

    return all_pass


if __name__ == '__main__':
    results = []
    results.append(('链接检查', test_html_links()))
    results.append(('结构检查', test_html_structure()))
    results.append(('标题一致性', test_title_consistency()))
    results.append(('侧栏一致性', test_admin_sidebar_consistency()))

    print(f"\n{'='*50}")
    print(f"=== 测试汇总 ===")
    for name, passed in results:
        status = '✅ 通过' if passed else '❌ 失败'
        print(f"  {status} - {name}")

    all_passed = all(r[1] for r in results)
    print(f"\n{'='*50}")
    if all_passed:
        print("🎉 所有测试通过!")
        sys.exit(0)
    else:
        print("💥 部分测试失败!")
        sys.exit(1)
