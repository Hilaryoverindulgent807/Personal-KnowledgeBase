import urllib.request, sys

url = 'http://localhost:8000/upload.html'
c = urllib.request.urlopen(url).read().decode('utf-8')

checks = [
    ('\u8d44\u6599\u6807\u9898', '\u8d44\u6599\u6807\u9898' in c and '\u62a5\u544a\u6807\u9898' not in c),
    ('\u4e00\u7ea7\u5206\u7c7b', '\u4e00\u7ea7\u5206\u7c7b' in c and '\u7814\u7a76\u9886\u57df' not in c),
    ('updateL2', 'updateL2' in c),
    ('L2_MAP', 'L2_MAP' in c),
    ('\u6218\u7565\u89c4\u5212', '\u6218\u7565\u89c4\u5212' in c),
    ('\u4f5c\u6218\u7406\u8bba', '\u4f5c\u6218\u7406\u8bba' in c),
    ('\u88c5\u5907\u53d1\u5c55', '\u88c5\u5907\u53d1\u5c55' in c),
    ('\u5173\u952e\u6280\u672f', '\u5173\u952e\u6280\u672f' in c),
    ('\u4f5c\u6218\u529b\u91cf', '\u4f5c\u6218\u529b\u91cf' in c),
    ('\u4f5c\u6218\u8fd0\u7528', '\u4f5c\u6218\u8fd0\u7528' in c),
    ('\u5178\u578b\u9879\u76ee', '\u5178\u578b\u9879\u76ee' in c),
    ('\u8d44\u6599\u7c7b\u578b', '\u8d44\u6599\u7c7b\u578b' in c),
    ('\u52a8\u6001\u4fe1\u606f/\u8bd1\u4e1b\u8bd1\u8457', '\u52a8\u6001\u4fe1\u606f' in c and '\u8bd1\u4e1b\u8bd1\u8457' in c),
    ('\u519b\u4e8b\u6587\u4ef6\u540d', '\u65e0\u4eba\u96c6\u7fa4' in c and '\u56fa\u6001\u7535\u6c60' not in c),
    ('\u519b\u4e8b\u9879\u76ee\u540d', '\u65e0\u4eba\u96c6\u7fa4\u667a\u80fd\u534f\u540c\u4e13\u9879' in c),
    ('\u519b\u4e8b\u90e8\u95e8\u540d', '\u6218\u7565\u60c5\u62a5\u7814\u7a76\u5ba4' in c),
    ('OFD\u683c\u5f0f', '.ofd' in c),
    ('\u4fe1\u606f\u5e93(no\u6210\u679c\u5e93)', '\u4fe1\u606f\u5e93' in c and '\u6210\u679c\u5e93' not in c),
    ('\u4fa7\u680f\u4e0b\u62c9', 'nav-group' in c),
    ('\u77e5\u8bc6\u56fe\u8c31(bottom)', 'sidebar-bottom' in c),
    ('\u4e8c\u7ea7\u5206\u7c7b', '\u4e8c\u7ea7\u5206\u7c7b' in c),
]

passed = 0
for label, ok in checks:
    status = 'PASS' if ok else 'FAIL'
    print(f'  {status} {label}')
    if ok: passed += 1

print(f'\n  Total: {passed}/{len(checks)} passed')
sys.exit(0 if passed == len(checks) else 1)
