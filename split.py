import re

html_file = 'src/main/resources/static/index.html'
css_file = 'src/main/resources/static/css/styles.css'
js_file = 'src/main/resources/static/js/app.js'
new_html_file = 'src/main/resources/static/index.html'

with open(html_file, 'r', encoding='utf-8') as f:
    content = f.read()

# Extract CSS
css_match = re.search(r'<style>(.*?)</style>', content, re.DOTALL)
css_content = css_match.group(1).strip() if css_match else ''

# Extract JS
js_match = re.search(r'<script>(.*?)</script>', content, re.DOTALL)
js_content = js_match.group(1).strip() if js_match else ''

# Clean HTML
new_html = re.sub(r'<style>.*?</style>', '<link rel="stylesheet" href="css/styles.css">', content, flags=re.DOTALL)
new_html = re.sub(r'<script>.*?</script>', '<script src="js/app.js"></script>', new_html, flags=re.DOTALL)

import os
os.makedirs('src/main/resources/static/css', exist_ok=True)
os.makedirs('src/main/resources/static/js', exist_ok=True)

with open(css_file, 'w', encoding='utf-8') as f:
    f.write(css_content)

with open(js_file, 'w', encoding='utf-8') as f:
    f.write(js_content)

with open(new_html_file, 'w', encoding='utf-8') as f:
    f.write(new_html)

print("Files successfully split.")
