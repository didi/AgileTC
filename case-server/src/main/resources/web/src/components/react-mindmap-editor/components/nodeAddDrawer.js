// 添加因子Modal
import React, { useState, useCallback } from 'react';
import { Drawer, Button, Row, Col, Input, Icon, Tooltip, Popconfirm, Select } from 'antd';

const { Option } = Select;

// 节点添加抽屉组件
const NoteAddDrawer = (props) => {
  const { visible, minder, onCancel } = props;
  const [list, setList] = useState([{ id: Date.now() }]); // 因子list
  const [selectVal, setVal] = useState('pariwise');

  // input发上变化
  const inputChange = useCallback((value, id, key) => {
    const newList = list;
    newList.find((item) => item.id === id)[key] = value;
    setList([...newList]);
  });
  // 删除
  const deleteList = (id) => {
    setList(list.filter((item) => item.id !== id));
  };
  // 上放下拉发生变化
  const handleChange = (value) => setVal(value);
  // 确认
  const submit = () => {
    let newList = {};
    list.map((item) => {
      newList[item.key] = item.value;
      return item;
    });
    window.ws.sendMessage('case_design_event', {method:selectVal, nodeId: minder.getSelectedNodes()[0].data.id, message: JSON.stringify(newList)}
      // `4|${selectVal}|${minder.getSelectedNodes()[0].data.id}|${JSON.stringify(newList)}`
    );
    onCancel();
  };

  return (
    <Drawer
      title={`${minder.getSelectedNodes()[0].data.text}`}
      placement="right"
      className="agiletc-note-drawer"
      maskClosable={false}
      onClose={onCancel}
      visible={visible}
      width="500"
    >
      <Row gutter={[0, 20]}>
        <Col span={5} style={{ lineHeight: '32px' }}>
          方法选择：
        </Col>
        <Col span={19}>
          <Select value={selectVal} onChange={handleChange} style={{ width: '100%' }}>
            <Option value="pariwise">pariwise</Option>
          </Select>
        </Col>
      </Row>

      {list.map((item, i) => (
        <Row gutter={[16, 16]} key={item.id}>
          <Col span={8}>
            <Input
              value={item.key}
              placeholder="请输入因子"
              onChange={(e) => inputChange(e.target.value, item.id, 'key')}
            />
          </Col>
          <Col span={list.length > 1 ? 14 : 16}>
            <Input
              value={item.value}
              placeholder="请输入因子取值并以“,”分割"
              onChange={(e) => inputChange(e.target.value, item.id, 'value')}
            />
          </Col>
          {list.length > 1 && (
            <Col span={2} style={{ color: 'red', marginTop: 5 }}>
              <Popconfirm
                title="确认删除?"
                placement="topRight"
                onConfirm={() => deleteList(item.id)}
                onCancel={(e) => console.log(e)}
                okText="确定"
                cancelText="取消"
              >
                <Icon type="delete" />
              </Popconfirm>
            </Col>
          )}
        </Row>
      ))}
      <div style={{ margin: '20px 0 70px' }}>
        <Tooltip
          placement="top"
          title={list.every((item) => item.key && item.value) ? null : '填写完整后方可继续添加'}
        >
          <Button
            type="dashed"
            block
            disabled={!list.every((item) => item.key && item.value)}
            onClick={() => setList([...list, { id: Date.now() }])}
          >
            添加
          </Button>
        </Tooltip>
      </div>
      <div
        style={{
          position: 'absolute',
          right: 0,
          bottom: 0,
          width: '100%',
          borderTop: '1px solid #e9e9e9',
          padding: '10px 16px',
          background: '#fff',
          textAlign: 'right'
        }}
      >
        <Button onClick={onCancel} style={{ marginRight: 8 }}>
          取消
        </Button>
        <Tooltip
          placement="topRight"
          title={list.every((item) => item.key && item.value) ? null : '填写完整后方可确认'}
        >
          <Button
            onClick={submit}
            type="primary"
            disabled={!list.every((item) => item.key && item.value)}
          >
            确认
          </Button>
        </Tooltip>
      </div>
    </Drawer>
  );
};
export default NoteAddDrawer;
