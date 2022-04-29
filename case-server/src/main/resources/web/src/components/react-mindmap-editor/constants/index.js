export const initData = {
  root: {
    data: {
      text: '百度产品',
      image: 'https://www.baidu.com/img/bd_logo1.png?where=super',
      imageSize: { width: 270, height: 129 }
    },
    children: [
      { data: { text: '新闻', note: 'asdadad' } },
      { data: { text: '网页', priority: 1 } },
      { data: { text: '贴吧', priority: 2 } },
      { data: { text: '知道', priority: 2 } },
      { data: { text: '音乐', priority: 0, progress: 1 } },
      { data: { text: '图片', priority: 3, progress: 5 } },
      { data: { text: '视频', priority: 3, progress: 0 } },
      { data: { text: '地图', priority: 3 } },
      {
        data: {
          created: 1587038237271,
          expandState: 'expand',
          'font-size': 18,
          'font-style': 'italic',
          'font-weight': 'bold',
          id: 'c22m3p7alqg0',
          note: 'lll',
          text: 'ddd'
        }
      },
      { data: { text: '百科', priority: 3 } },
      { data: { text: '更多', hyperlink: 'http://www.baidu.com/more' } }
    ]
  }
}

// hotbox 操作list
export const buttons = [
  '前移:Alt+Up:ArrangeUp',
  '下级:Tab|Insert:AppendChildNode',
  '同级:Enter:AppendSiblingNode',
  '后移:Alt+Down:ArrangeDown',
  '删除:Delete|Backspace:RemoveNode',
  '上级:Shift+Tab|Shift+Insert:AppendParentNode'
]
// 外观tab 主题list
export const theme = {
  classic: '脑图经典',
  'classic-compact': '紧凑经典',
  snow: '温柔冷光',
  'snow-compact': '紧凑冷光',
  fish: '鱼骨图',
  wire: '线框',
  'fresh-red': '清新红',
  'fresh-soil': '泥土黄',
  'fresh-green': '文艺绿',
  'fresh-blue': '天空蓝',
  'fresh-purple': '浪漫紫',
  'fresh-pink': '脑残粉',
  'fresh-red-compat': '紧凑红',
  'fresh-soil-compat': '紧凑黄',
  'fresh-green-compat': '紧凑绿',
  'fresh-blue-compat': '紧凑蓝',
  'fresh-purple-compat': '紧凑紫',
  'fresh-pink-compat': '紧凑粉',
  tianpan: '经典天盘',
  'tianpan-compact': '紧凑天盘'
}
// 外观tab 字号list
export const fontSizeList = [10, 12, 16, 18, 24, 32, 48]
// 外观tab 字体list
export const fontFamilyList = [
  {
    name: '宋体',
    val: '宋体,SimSun'
  },
  {
    name: '微软雅黑',
    val: '微软雅黑,Microsoft YaHei'
  },
  {
    name: '楷体',
    val: '楷体,楷体_GB2312,SimKai'
  },
  {
    name: '黑体',
    val: '黑体, SimHei'
  },
  {
    name: '隶书',
    val: '隶书, SimLi'
  },
  {
    name: 'Andale Mono',
    val: 'andale mono'
  },
  {
    name: 'Arial',
    val: 'arial,helvetica,sans-serif'
  },
  {
    name: 'arialBlack',
    val: 'arial black,avant garde'
  },
  {
    name: 'Comic Sans Ms',
    val: 'comic sans ms'
  },
  {
    name: 'Impact',
    val: 'impact,chicago'
  },
  {
    name: 'Times New Roman',
    val: 'times new roman'
  },
  {
    name: 'Sans-Serif',
    val: 'sans-serif'
  }
]
export const template = {
  default: '思维导图',
  tianpan: '天盘图',
  structure: '组织结构图',
  filetree: '目录组织图',
  right: '逻辑结构图',
  'fish-bone': '鱼骨头图'
}
// 视图tab 展开list
export const expandToList = {
  1: '展开到一级节点',
  2: '展开到二级节点',
  3: '展开到三级节点',
  4: '展开到四级节点',
  5: '展开到五级节点',
  6: '展开到六级节点',
  9999: '展开全部节点'
}
// 视图tab 全选list
export const selectedList = {
  all: '全选',
  revert: '反选',
  siblings: '选择兄弟节点',
  level: '选择同级节点',
  path: '选择路径',
  tree: '选择子树'
}
// 放大缩小比例
export const zoom = [10, 20, 30, 40, 50, 60, 80, 100, 120, 150, 180, 200]
