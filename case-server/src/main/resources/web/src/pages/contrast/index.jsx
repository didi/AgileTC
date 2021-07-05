/** 历史版本对比 */
import React from 'react'
import { Table, Button, Card, Tooltip } from 'antd'
import moment from 'moment'
import './index.scss'
import Headers from '../../layouts/headers'
import request from '@/utils/axios'
moment.locale('zh-cn')

class Contrast extends React.Component {
  constructor(props) {
    super(props)
    this.state = {
      rowKeys: [], // 当前选择行的 key
      rows: [], // 当前选择的行数据
      historyList: [],
    }
  }
  componentDidMount() {
    this.getHistoryList()
  }
  getHistoryList = () => {
    request(`/backup/getBackupByCaseId`, {
      method: 'GET',
      params: {
        caseId: this.props.match.params.caseId,
      },
    }).then(res => {
      if (res.code === 200) {
        this.setState({ historyList: res.data })
      }
    })
  }
  contrastClick = () => {
    const { rows } = this.state
    this.props.history.push(`/caseManager/historyContrast/${rows[0].id}/${rows[1].id}`)
  }
  setTableColums = () => {
    const columns = [
      {
        title: '备份ID',
        dataIndex: 'id',
      },
      {
        title: '创建时间',
        dataIndex: 'gmtCreated',
        render: text => {
          return <span>{moment(text).format('YYYY-MM-DD HH:mm:ss')}</span>
        },
      },
      {
        title: '创建人',
        dataIndex: 'creator',
      },
    ]
    return columns
  }
  render() {
    const rowSelection = {
      onChange: (selectedRowKeys, selectedRows) => {
        this.setState({ rowKeys: selectedRowKeys, rows: selectedRows })
      },
      getCheckboxProps: record => ({
        disabled: this.state.rowKeys.length >= 2 && !this.state.rowKeys.includes(record.id),
        name: record.name,
      }),
    }
    return (
      <section style={{ marginBottom: 30 }}>
        <Headers />
        <Card
          bordered={false}
          className={this.state.rowKeys.length >= 2 ? 'contras_card' : 'contras_card_default'}
        >
          <div className="contras_title">
            {/* <span>历史版本</span> */}
            <Tooltip
              placement="top"
              title={this.state.rowKeys.length < 2 ? '选择两个版本后，才可以对比哦～' : null}
            >
              <Button
                type="primary"
                disabled={this.state.rowKeys.length < 2}
                onClick={this.contrastClick}
              >
                对比已选择版本
              </Button>
            </Tooltip>
          </div>
          <Table
            rowKey="id"
            rowSelection={rowSelection}
            columns={this.setTableColums()}
            dataSource={this.state.historyList}
          />
        </Card>
      </section>
    )
  }
}
export default Contrast
