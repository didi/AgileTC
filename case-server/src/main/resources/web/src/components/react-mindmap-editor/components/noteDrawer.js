import React, { useState, useCallback, useEffect, forwardRef } from 'react';
import { Drawer } from 'antd';
import Editor from 'for-editor';
import debounce from 'lodash/debounce';

// 备注抽屉组件
const NoteDrawer = (props) => {
  const { minder, onCancel, visible, noteStatus, toolbar = {} } = props;
  const [value, setValue] = useState('');
  // const [percent, setPercent] = useState(0);

  const debouncedUpdate = useCallback(
    debounce((newValue) => minder.execCommand('Note', newValue), 500),
    []
  );

  const handleChange = (newValue) => {
    setValue(newValue);
    debouncedUpdate(newValue);
  };

  // const handleUpload = (e) => {
  //   const { status, response } = e.file;
  //   if (status === 'done') {
  //     setPercent(0);
  //     if (response.success) {
  //       const fileName = response.data[0] ? response.data[0].name : '';
  //       const fileUrl = response.data[0] ? response.data[0].url : '';
  //       const imageContent = `![${fileName}](${fileUrl})`;
  //       handleChange(`${value}\n${imageContent}`);
  //     } else {
  //       message.error(response.msg);
  //     }
  //   } else if (status === 'uploading') {
  //     setPercent(e.file.percent);
  //   } else if (status === 'error') {
  //     setPercent(0);
  //     message.error(e.file.error ? e.file.error.message : '');
  //   }
  // };

  useEffect(() => {
    setValue(minder.queryCommandValue('note') || toolbar.noteTemplate || '');
  }, [noteStatus]);

  useEffect(() => {
    setValue(
      minder.getSelectedNode()
        ? minder.getSelectedNode().data.note || toolbar.noteTemplate || ''
        : ''
    );
  }, [minder.getSelectedNode()]);

  return (
    <Drawer
      title={<span>备注（支持 Markdown 语法）</span>}
      placement="right"
      width={360}
      onClose={onCancel}
      visible={visible}
      mask={false}
      getContainer={() => minder.getPaper().container.parentNode}
      bodyStyle={{ padding: 0 }}
      className="agiletc-note-drawer"
    >
      <Editor
        toolbar={{}}
        style={{ borderRadius: 0, border: 'none', height: 'calc(100vh - 55px)' }}
        value={value}
        onChange={handleChange}
      />
      {/* <div style={{ backgroundColor: '#eee', height: 40 }}>
        <Upload
          action={baseUrl + uploadUrl}
          listType="picture"
          accept="image/*"
          withCredentials
          showUploadList={false}
          onChange={handleUpload}
        >
          <Button icon="upload" size="large" type="link">
            点击上传并插入图片 {percent > 0 ? `${parseInt(percent)}%` : ''}
          </Button>
        </Upload>
      </div> */}
    </Drawer>
  );
};
export default forwardRef((props, ref) => <NoteDrawer {...props} ref={ref} />);
