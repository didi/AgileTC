/** 历史版本对比结果 */
import React from 'react'
import AgileTCEditor from 'react-agiletc-editor'
import { message, Spin, Card, Tag } from 'antd'
import moment from 'moment'
moment.locale('zh-cn')
import request from '@/utils/axios'
import Headers from '../../layouts/headers'
import './index.scss'

class SeeResult extends React.Component {
  constructor(props) {
    super(props)
    this.state = {
      loading: false,
      info: [],
    }
  }
  componentDidMount() {
    this.setState({ loading: true })
    request(`/backup/getCaseDiff`, {
      method: 'GET',
      params: {
        caseId1: this.props.match.params.caseId1,
        caseId2: this.props.match.params.caseId2,
      },
    }).then(res => {
      this.setState({ loading: false })
      if (res.code === 200) {
        this.editorNode.setEditerData(res.data.content.root)
        this.setState({ info: res.data.backupinfo })
      } else {
        message.error(res.msg)
      }
    })
  }
  render() {
    return (
      <React.Fragment>
        <Headers />
        <Spin tip="Loading..." spinning={this.state.loading}>
          <div className="historyBox">
            <div className="box_title">
              <Card bordered={false} title="版本比较" className="title_history">
                <div style={{ display: 'flex' }}>
                  {this.state.info &&
                    this.state.info.map((item, i) => (
                      <Card style={{ marginRight: 20 }} key={i}>
                        <span>
                          创建人: {item.user}
                          <br />
                          创建时间: {moment(item.time).format('YYYY-MM-DD HH:mm:ss')}
                        </span>
                      </Card>
                    ))}
                </div>
              </Card>
              <Card bordered={false} title="颜色标识" className="title_color">
                <Tag color="#ddfade">该节点被添加</Tag>
                <Tag color="#ffe7e7">该节点被删除</Tag>
                <Tag color="#d6f0ff">内容已变更</Tag>
              </Card>
            </div>
            <AgileTCEditor
              ref={editorNode => (this.editorNode = editorNode)}
              tags={['前置条件', '执行步骤', '预期结果']}
              progressShow={true}
              readOnly={true}
              mediaShow={true}
              editorStyle={{ height: 'calc(100vh - 240px)' }}
              toolbar={{
                image: true,
                theme: ['classic-compact', 'fresh-blue', 'fresh-green-compat'],
                template: ['default', 'right', 'fish-bone'],
                noteTemplate: '# test',
              }}
              uploadUrl="/api/file/uploadAttachment"
              wsUrl="ws://localhost:8094/api/case/2227/undefined/0/user"
              type="compare"
            />
          </div>
        </Spin>
      </React.Fragment>
    )
  }
}
export default SeeResult
