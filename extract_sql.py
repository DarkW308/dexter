import os
import glob
import re

md_files = glob.glob('dexter_*.md')
all_sql = []

for file in md_files:
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
        # Find all SQL code blocks
        sql_blocks = re.findall(r'`sql(.*?)`', content, re.DOTALL | re.IGNORECASE)
        if sql_blocks:
            all_sql.append(f'-- From {file}\n')
            for block in sql_blocks:
                all_sql.append(block.strip() + '\n')

with open('dexter/schema.sql', 'w', encoding='utf-8') as f:
    f.write('\n'.join(all_sql))

print(f'Extracted SQL into dexter/schema.sql. Total files processed: {len(md_files)}')
