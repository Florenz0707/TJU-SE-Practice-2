import re

paths = [
    r'D:\partical\TJU-SE-Practice=2\elm-cloud\points-service\src\main\java\cn\edu\tju\points\controller\PointsAdminController.java',
    r'D:\partical\TJU-SE-Practice=2\elm-cloud\points-service\src\main\java\cn\edu\tju\points\controller\PointsController.java'
]

for path in paths:
    with open(path, 'r', encoding='utf-8', errors='ignore') as f:
        text = f.read()
    
    text = re.sub(r'ResultCodeEnum\.FORBIDDEN,\s*\"[^\)]*\);', 'ResultCodeEnum.FORBIDDEN, "Forbidden");', text)
    text = re.sub(r'ResultCodeEnum\.([A-Z_]+),\s*\"([^\"]*)\);', r'ResultCodeEnum.\1, "\2");', text)
    text = re.sub(r'ResultCodeEnum\.([A-Z_]+),\s*\"([^\"]*)$', r'ResultCodeEnum.\1, "Error");', text, flags=re.MULTILINE)
    text = re.sub(r'HttpResult\.success\(\"([^\"]*)\);', r'HttpResult.success("\1");', text)
    text = re.sub(r'HttpResult\.success\(\"([^\"]*)$', r'HttpResult.success("Success");', text, flags=re.MULTILINE)
    text = re.sub(r'\"[^\"]*\);', '");', text)  # Just clean any stray unclosed `garbled);`

    with open(path, 'w', encoding='utf-8') as f:
        f.write(text)
    print('Fixed formatting in', path)