#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
把老系统 7 张 .mrt 体检表整理成「结构化可编辑模板」的种子数据。

背景：mix-ui/public/mrt/Checkup*.json 是 mrt2json.py 从 Stimulsoft 报表导出的
绝对坐标文本快照，院名写死、文字错位、无法录入。本脚本按语义重新归纳成
「分区 → 项目」树，医生可在管理端继续增删改。

输出：db-backup/feature_20261114_checkup.sql
  1) checkup_template / checkup_record 两张表 DDL
  2) 7 条内置模板种子（INSERT ... WHERE NOT EXISTS，幂等且不覆盖用户后续编辑）

item.type:
  check     勾选（可带 side=lr|four → 左右/四肢各一个勾选）
  choice    单选一组选项（可带 side=lr → 左右各选一个）
  number    数值输入（unit 单位）
  text      单行文本
  textarea  多行文本
  note      纯说明文字（不可填，用于打印时的医嘱/提示）
item.normal: 标记该项为「正常」默认值（录入时一键置正常）
"""
import json
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
OUT = os.path.join(ROOT, 'db-backup', 'feature_20261114_checkup.sql')


def C(label, side=None, normal=False):
    d = {'label': label, 'type': 'check'}
    if side:
        d['side'] = side
    if normal:
        d['normal'] = True
    return d


def CH(label, options, side=None):
    d = {'label': label, 'type': 'choice', 'options': options}
    if side:
        d['side'] = side
    return d


def N(label, unit=''):
    return {'label': label, 'type': 'number', 'unit': unit}


def T(label, multi=False):
    return {'label': label, 'type': 'textarea' if multi else 'text'}


def NOTE(text):
    return {'label': text, 'type': 'note'}


def S(title, items):
    return {'title': title, 'items': items}


TEMPLATES = [
    {
        'key': 'CheckupTijian', 'name': '基础体检表', 'category': '常规体检',
        'description': '全身 14 个系统的常规体格检查，逐项勾选，含左右/四肢与体征数值',
        'sections': [
            S('1. 皮肤与毛发', [
                C('正常', normal=True), C('暗淡无光/干燥'), C('疥癣'), C('红斑'), C('搔痒'),
                C('脱皮'), C('打结'), C('肿瘤'), C('跳蚤感染'), C('细菌感染'),
                C('掉毛'), C('色素沉着'), T('其他'),
            ]),
            S('2. 眼睛', [
                C('正常', normal=True), C('有分泌物', 'lr'), C('发炎', 'lr'), C('眼睑畸形', 'lr'),
                C('感染', 'lr'), C('白内障', 'lr'), C('晶状体硬化', 'lr'), T('其他'),
            ]),
            S('3. 耳朵', [
                C('正常', normal=True), C('发炎'), C('搔痒'), C('耳螨'), C('肿瘤', 'lr'),
                C('毛发过多'), C('酵母菌', 'lr'), C('细菌感染', 'lr'), T('其他'),
            ]),
            S('4. 鼻子和咽喉', [
                C('正常', normal=True), C('鼻道有分泌物'), C('咽喉发炎'), C('扁桃体发炎'),
                C('淋巴腺肿大'), C('充血'), T('其他'),
            ]),
            S('5. 口腔、牙齿、牙龈', [
                C('正常', normal=True), C('牙齿断裂'), C('牙结石'), C('溃疡'),
                C('牙龈发炎'), C('牙齿脱落'), C('压槽脓肿'), C('牙龈肿胀'),
            ]),
            S('6. 腿和爪子', [
                C('正常', normal=True), C('跛行', 'four'), C('韧带拉伤'),
                C('指甲过长'), C('关节问题'), C('脚和毛发变色'),
            ]),
            S('7. 心脏', [
                C('正常', normal=True), C('心脏杂音'), C('心跳慢'), C('心跳快'), T('其他'),
            ]),
            S('8. 腹部', [
                C('正常', normal=True), C('增大'), C('腹水'), C('腹部异物'),
                C('紧张/疼痛'), C('有杂音'), T('其他'),
            ]),
            S('9. 肺', [
                C('正常', normal=True), C('有杂音'), C('咳嗽'), C('呼吸困难'),
                C('呼吸急促'), T('其他'),
            ]),
            S('10. 胃消化系统', [
                C('正常', normal=True), C('呕吐'), C('排便不正常'), C('胀气'),
                C('厌食'), C('寄生虫'), T('其他'),
            ]),
            S('11. 泌尿生殖系统', [
                C('正常', normal=True), C('排尿异常'), C('生殖器有分泌物'), C('睾丸异常'),
                C('乳腺肿瘤'), C('肛门腺囊肿大'), C('前列腺肿大'),
            ]),
            S('12. 体重', [
                C('正常范围', normal=True), N('超重', 'Kg'), N('超轻', 'Kg'), N('其他', 'Kg'),
            ]),
            S('13. 食物', [
                C('优质品'), C('好的'), C('需要补充维他命'), C('正常', normal=True), N('体温', '℃'),
            ]),
            S('14. 专项说明', [
                N('心率', '次/分'), N('CRT', 's'), N('瞳孔'), N('呼吸', '次/分'), T('说明', True),
            ]),
        ],
    },
    {
        'key': 'CheckupPifu', 'name': '皮肤检查表', 'category': '专科检查',
        'description': '皮肤病专科问诊：来源、生活环境、既往史、发病经过与季节性',
        'sections': [
            S('基本信息', [
                CH('该宠物来源', ['宠物店', '繁殖场', '家庭']), T('家庭地理位置（过去及目前）'),
                T('生活环境与方式（家庭及常去的地方）'), T('是否有过旅行寄养或美容'),
                T('接触其它宠物情况'), T('精神状态及行为改变记录'), T('饮食种类及状况'),
                T('排尿情况'), T('排便情况'), T('洗澡及浴液情况'), T('生殖情况'),
                T('曾患疾病及手术'), T('现是否有除皮肤病以外的疾病'),
            ]),
            S('皮肤病病史记录', [
                T('主述', True), T('初始发病日期'), T('初始发病部位及症状'),
                CH('发病开始情况', ['突然发病', '逐渐发病']),
                CH('疾病曾经是否有季节性', ['是', '否']),
                CH('曾经季节性', ['春', '夏', '秋', '冬']),
                CH('现在是否有季节性', ['是', '否']),
                T('既往用药及疗效'), T('瘙痒程度及部位'), T('皮损分布'),
            ]),
            S('皮肤检查所见', [
                T('皮损类型（红斑/丘疹/脓疱/鳞屑/结痂/脱毛/色素沉着）'),
                T('分布部位'), T('拔毛/刮片/伍德氏灯检查'), T('真菌培养'),
                T('初步诊断'), T('处置与用药'), T('医嘱', True),
            ]),
        ],
    },
    {
        'key': 'CheckupYachi', 'name': '牙齿检查表', 'category': '专科检查',
        'description': '口腔常见症状自查清单 + 洗牙医嘱',
        'sections': [
            S('常见症状（逐项勾选）', [
                C('嘴里长时间有口臭'), C('牙齿上有结石'), C('牙龈红肿'), C('烦躁，易怒'),
                C('牙齿变色'), C('牙齿已松动、脱落'), C('食欲减退'),
                C('吃东西时喜欢用一侧牙齿咀嚼'), C('不愿意玩啃咬玩具'),
                C('嘴部红肿、较敏感'), C('进食或啃咬困难，喜欢吃流食'),
                C('精神抑郁'), C('经常用爪子挠嘴'),
            ]),
            S('医嘱', [
                NOTE('1. 每天清洁牙齿；'), NOTE('2. 定期洗牙；'),
                T('3. 下次洗牙时间'),
            ]),
            S('说明', [T('说明', True)]),
        ],
    },
    {
        'key': 'CheckupOral', 'name': '口腔检查表', 'category': '专科检查',
        'description': '口腔/牙科诊疗记录：主诉、病史、诊断、治疗与居家保健建议',
        'sections': [
            S('基本信息', [
                T('饲主姓名'), T('宠物名'), T('日期'), T('品种'), T('性别'),
                CH('结扎', ['是', '否', '不知']), T('年龄'), T('诊察兽医师'),
            ]),
            S('诊疗记录', [
                T('就诊症状', True), T('医疗记录', True), T('过去病史/症状', True),
                T('诊断'), T('治疗', True), T('洗牙/抛光/涂氟'),
                T('牙周病/牙根面整平/牙结石刮除/补牙'), T('X 光'),
                CH('定期治疗', ['是', '否']), CH('延续治疗', ['是', '否']),
                T('建议事项', True),
            ]),
            S('用药与居家保健', [
                T('抗生素'), T('止疼药'), T('食物建议', True),
                CH('啃咬玩具', ['是', '否']), T('居家保健-刷牙'),
                CH('刷牙液/刷牙凝胶', ['是', '否']), T('品名'),
                CH('复诊', ['是', '否']), T('复诊日期'),
            ]),
        ],
    },
    {
        'key': 'CheckupYankeh', 'name': '眼科检查表', 'category': '专科检查',
        'description': '双眼分列的眼科检查：眼睑、角膜、晶状体、眼压、泪液量等',
        'sections': [
            S('眼前段（左右分列）', [
                CH('眼睑', ['正常', '异常'], 'lr'), CH('巩膜', ['正常', '异常'], 'lr'),
                CH('结膜', ['正常', '冲血', '黄染'], 'lr'), CH('角膜', ['正常', '异常'], 'lr'),
                CH('晶状体', ['正常', '白内障', '硬化'], 'lr'),
                CH('虹膜', ['正常', '异常'], 'lr'),
                CH('瞳孔', ['正常', '异常', '缩小', '放大', '轻微反射', '无反射'], 'lr'),
                CH('睫状体', ['正常', '异常'], 'lr'),
            ]),
            S('眼功能检查（左右分列）', [
                N('泪液量', 'mm/min'), N('泪液量（右）', 'mm/min'),
                CH('眼球位置', ['正常', '内陷', '突出'], 'lr'),
                N('眼内压 IOP', 'mmHg'), N('眼内压 IOP（右）', 'mmHg'),
                CH('视力/威胁反射', ['正常', '异常'], 'lr'),
                CH('荧光素染色', ['阴性', '阳性'], 'lr'),
            ]),
            S('结论', [T('诊断', True), T('处置与医嘱', True)]),
        ],
    },
    {
        'key': 'CheckupPatho', 'name': '理学检查记录表', 'category': '常规体检',
        'description': '生活习惯问卷 + 全身体格理学检查（老系统「病理检查表」）',
        'sections': [
            S('生活习惯', [
                CH('精神状态', ['正常', '兴奋', '倦怠', '紧张']),
                CH('食欲', ['正常', '极佳', '尚可', '不吃']),
                CH('平日食物', ['干料', '罐头', '人吃', '不一定', '其他']),
                T('食物品牌'), CH('一日餐数', ['一餐', '两餐', '三餐', '不一定']),
                CH('排便习惯', ['笼内', '沙盆', '户外', '室内', '其他']),
                CH('粪便外观', ['正常', '干便', '泥状', '水状']),
                CH('活动范围', ['室内', '户外', '自由']),
                CH('洗澡间隔', ['三天', '每周', '两周', '每月', '不一定']),
                CH('临床症状', ['正常', '呕吐', '下痢', '咳嗽', '喷嚏', '其他']),
            ]),
            S('物理检查', [
                CH('营养状态', ['正常', '稍瘦', '稍胖', '过瘦', '过胖', '不良']),
                N('体温', '℃'), CH('体温判定', ['正常', '发烧', '失温']),
                CH('粪检', ['正常', '分泌物', '虫卵']),
                CH('眼睛', ['正常', '分泌物', '溃疡'], 'lr'),
                CH('耳朵', ['正常', '分泌物', '发炎'], 'lr'),
                CH('皮肤', ['正常', '脱毛', '皮屑', '红肿']),
                CH('口腔', ['正常', '牙结石', '牙龈炎']),
                CH('心肺', ['正常', '杂音', '心律不整']),
                CH('腹部触诊', ['正常', '紧张', '肿块']),
                CH('淋巴结', ['正常', '肿大']),
                N('心率', '次/分'), N('呼吸', '次/分'), N('体重', 'Kg'),
            ]),
            S('结论', [T('初步诊断', True), T('建议与医嘱', True)]),
        ],
    },
    {
        'key': 'CheckupHealth', 'name': '健康检查表（出境）', 'category': '法定/出境',
        'description': '伴侣动物出境健康检查表：主人/宠物/芯片/免疫史/临床检查，中英对照',
        'sections': [
            S('宠物主人信息 Owner Information', [
                T('宠物主人姓名 Owners name'), T('联系电话 Telephone'),
                T('居住地址 Address of Owner'), T('运输方式 Mode of Transport'),
                T('启运地 Place of Despatch'), T('到达国家/地区 Country/Region of Destination'),
                T('离境日期 Date of Departure'),
            ]),
            S('宠物信息 Animal Information', [
                T('宠物种类 Species'), T('品种 Breed'), T('性别 Sex'), T('颜色 Color'),
                T('年龄/出生年月 Age/DOB'), N('体重 Weight', 'kg'),
            ]),
            S('芯片信息 Microchip Information', [
                T('芯片类型 Microchip Standard'), T('芯片植入日期 Date of Insertion'),
                T('芯片号码 Number of Microchip'),
            ]),
            S('免疫史 The Vaccination Records', [
                T('最近一次疫苗接种时间'), T('疫苗种类 Type of Vaccine'), T('失效日期'),
                T('倒数第二次疫苗接种时间'), T('倒数第二次疫苗种类'), T('倒数第二次失效日期'),
                T('血液采样日期 Date of The Blood Sample'), T('狂犬病抗体检测结果'),
            ]),
            S('临床检查 Physical Exam', [
                N('心率 Heart Rate', '次/分'), N('呼吸频率 Respiration Rate', '次/分'),
                N('体温 Temperature', '℃'), CH('总体状况', ['正常 Normal', '异常 Abnormal']),
                T('临床检查结论', True), T('兽医签名与日期'),
            ]),
        ],
    },
]


def build():
    for t in TEMPLATES:
        for si, s in enumerate(t['sections'], 1):
            s['key'] = f's{si}'
            for ii, it in enumerate(s['items'], 1):
                it['key'] = f's{si}_i{ii}'
        t['schema'] = {'title': t['name'], 'sections': t['sections']}


def sql_str(v):
    return "'" + str(v).replace('\\', '\\\\').replace("'", "''") + "'"


DDL = """-- 体检表可编辑模板 + 结构化体检记录（2026-11-14）
-- 由 migration/tools/checkup_seed.py 生成；老 7 种 mrt 版式保留只读，新旧并行。
CREATE TABLE IF NOT EXISTS checkup_template (
  template_id INT AUTO_INCREMENT PRIMARY KEY,
  tpl_key VARCHAR(64) NOT NULL COMMENT '模板键（沿用老 mrt 短名）',
  name VARCHAR(64) NOT NULL COMMENT '模板名',
  category VARCHAR(32) NOT NULL DEFAULT '' COMMENT '分类',
  description VARCHAR(255) NOT NULL DEFAULT '' COMMENT '用途说明',
  schema_json LONGTEXT NOT NULL COMMENT '分区/项目树 JSON',
  active TINYINT NOT NULL DEFAULT 1 COMMENT '启用',
  is_builtin TINYINT NOT NULL DEFAULT 1 COMMENT '内置种子（可编辑，但迁移不覆盖）',
  sort_order INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_del BIGINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_tpl_key (tpl_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='体检表可编辑模板';

CREATE TABLE IF NOT EXISTS checkup_record (
  checkup_id INT AUTO_INCREMENT PRIMARY KEY,
  template_id INT DEFAULT NULL,
  tpl_key VARCHAR(64) DEFAULT NULL,
  template_name VARCHAR(64) NOT NULL DEFAULT '' COMMENT '模板名快照',
  schema_json LONGTEXT NOT NULL COMMENT '模板快照：改模板不影响历史报告',
  results_json LONGTEXT DEFAULT NULL COMMENT '逐项结果，key=item.key',
  meta_json LONGTEXT DEFAULT NULL COMMENT '主人/宠物/医院/医生等表头快照',
  conclusion TEXT COMMENT '体检结论',
  record_id INT DEFAULT NULL COMMENT '关联病历（可为空：单独体检）',
  pet_id INT NOT NULL,
  client_id INT DEFAULT NULL,
  doctor_id INT DEFAULT NULL,
  check_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_by VARCHAR(64) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_del BIGINT NOT NULL DEFAULT 0,
  KEY idx_pet (pet_id), KEY idx_record (record_id), KEY idx_time (check_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结构化体检记录';
"""


def main():
    build()
    lines = [DDL, '', '-- 7 个内置模板：仅在不存在的键时插入，幂等且不会覆盖管理员后续的编辑']
    for i, t in enumerate(TEMPLATES, 1):
        schema = json.dumps(t['schema'], ensure_ascii=False, separators=(',', ':'))
        lines.append(
            "INSERT INTO checkup_template (tpl_key,name,category,description,schema_json,active,is_builtin,sort_order)\n"
            f"SELECT {sql_str(t['key'])},{sql_str(t['name'])},{sql_str(t['category'])},"
            f"{sql_str(t['description'])},{sql_str(schema)},1,1,{i}\n"
            f"WHERE NOT EXISTS (SELECT 1 FROM checkup_template WHERE tpl_key={sql_str(t['key'])});"
        )
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines) + '\n')
    print(f'生成 {OUT}')
    for t in TEMPLATES:
        n = sum(len(s['items']) for s in t['sections'])
        print(f"  {t['key']:<18} {t['name']:<20} 分区 {len(t['sections']):>2} 项 {n:>3}")


if __name__ == '__main__':
    main()
