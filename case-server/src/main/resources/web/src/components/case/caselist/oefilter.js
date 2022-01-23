/* eslint-disable */
import React from 'react';
import { Row, Col, Select, Input, DatePicker, Button, Spin, Icon } from 'antd';
import './index.scss';
import moment from 'moment';
moment.locale('zh-cn');
const { RangePicker } = DatePicker;
import request from '@/utils/axios';
const Option = Select.Option;
import debounce from 'lodash/debounce';
class OeFilter extends React.Component {
  static propTypes = {};
  constructor(props) {
    super(props);
    this.state = {
      choiseDate: [],
      iterationList: [], // 需求列表
      iterationFilter: [],
      createrFilter: [],
      current: 1,
      nameFilter: '',
      fetching: false,
      data: [],
      value: [],
      caseKeyWords: '',
      requirementOe: [],
    };
    this.lastFetchId = 0;
    this.getOeRequirement = debounce(this.getOeRequirement, 800);
  }
  componentWillReceiveProps(nextProp) {
    if (
      this.props.visibleDrawer != nextProp.visibleDrawer &&
      !nextProp.visibleDrawer
    ) {
      this.setState({
        iterationFilter: '',
        nameFilter: '',
        choiseDate: [],
        createrFilter: '',
        current: 1,
        value: [],
        caseKeyWords: '',
      });
    }
  }
  nameFiltersInput = e => {
    this.setState({ nameFilter: e.target.value });
  };
  onDataChange = (value, dateString) => {
    this.setState({ choiseDate: dateString });
  };
  unDoFilter = () => {
    this.setState(
      {
        iterationFilter: '',
        nameFilter: '',
        choiseDate: [],
        createrFilter: '',
        current: 1,
        value: [],
        caseKeyWords: '',
      },
      () => {
        const {
          current,
          nameFilter,
          createrFilter,
          iterationFilter,
          choiseDate,
          caseKeyWords,
        } = this.state;
        this.props.getCaseList(
          current,
          nameFilter,
          createrFilter,
          iterationFilter,
          choiseDate,
          caseKeyWords,
        );
      },
    );
  };
  doFilter = () => {
    this.setState({ current: 1 }, () => {
      const {
        current,
        nameFilter,
        createrFilter,
        iterationFilter,
        choiseDate,
        caseKeyWords,
      } = this.state;
      this.props.getCaseList(
        current,
        nameFilter,
        createrFilter,
        iterationFilter,
        choiseDate,
        caseKeyWords,
      );
    });
  };
  createPropleFilter = value => {
    this.setState({ createrFilter: value });
  };

  getOeRequirement = title => {
    this.setState({ fetching: true });
    request(
      `${this.props.oeApiPrefix}/business-lines/${this.props.productId}/requirements`,
      { method: 'GET', params: { title: title, pageNum: 1, pageSize: 25 } },
    ).then(res => {
      let { requirementDetails } = res;
      this.lastFetchId += 1;
      const fetchId = this.lastFetchId;
      if (fetchId !== this.lastFetchId) {
        return;
      }
      this.setState({ requirementOe: requirementDetails, fetching: false });
    });
  };
  handleChange = value => {
    // let val = value.map(item => item.key.split('-')[0]).join(',');

    this.setState({
      value,
      data: [],
      iterationFilter: value,
      fetching: false,
    });
  };
  caseKeyWordsChange = value => {
    this.setState({
      caseKeyWords: value,
    })
  }
  render() {
    const { choiseDate, value, fetching, caseKeyWords } = this.state;
    const { productMember, filterStatus, closeFilter } = this.props;
    return (
      <div className={`filter-case-modal-wrapper ${filterStatus}`}>
        <div className="filter-case-header">
          <span>快速筛选</span>
          <Icon onClick={closeFilter} type="close" />
        </div>
        <Row>
          <Col span={24} className="m-b-24">
            <div className="filter-item">
              <Input
                placeholder="用例集名称"
                style={{ width: '100%' }}
                onChange={this.nameFiltersInput}
                value={this.state.nameFilter}
              />
            </div>
          </Col>
          <Col span={24} className="m-b-24">
            <div className="filter-item">
              <Select
                showSearch
                allowClear
                style={{ width: '100%' }}
                placeholder="创建人"
                optionFilterProp="children"
                onChange={this.createPropleFilter}
                value={
                  this.state.createrFilter
                    ? this.state.createrFilter
                    : undefined
                }
                filterOption={(input, option) =>
                  option.props.children
                    .toLowerCase()
                    .indexOf(input.toLowerCase()) >= 0
                }
              >
                {productMember.map((item, index) => {
                  return (
                    <Option key={index} value={item.staffNamePY}>
                      {item.staffNameCN}
                    </Option>
                  );
                })}
              </Select>
            </div>
          </Col>
          <Col span={24} className="m-b-24">
            <div className="filter-item">
              <Input
                value={value}
                placeholder="关联需求"
                onChange={e => this.handleChange(e.target.value)}
                style={{ width: '100%' }}
              />
            </div>
          </Col>
          <Col span={24} className="m-b-24">
            <div className="filter-item">
              <Input
                value={caseKeyWords}
                placeholder="用例关键词"
                onChange={e => this.caseKeyWordsChange(e.target.value)}
                style={{ width: '100%' }}
              />
            </div>
          </Col>
          <Col span={24} className="m-b-24">
            <div className="filter-item ">
              <RangePicker
                value={
                  choiseDate[0]
                    ? [
                        moment(choiseDate[0], 'YYYY-MM-DD'),
                        moment(choiseDate[1], 'YYYY-MM-DD'),
                      ]
                    : null
                }
                format={'YYYY-MM-DD'}
                placeholder={['开始时间', '结束时间']}
                onChange={this.onDataChange}
                style={{ width: '100%' }}
              />
            </div>
          </Col>
        </Row>
        <div className="button-bottom">
          <Button onClick={this.unDoFilter} style={{ marginRight: 8 }}>
            重置
          </Button>
          <Button onClick={this.doFilter} type="primary">
            搜索
          </Button>
          &nbsp; &nbsp;
        </div>
      </div>
    );
  }
}
export default OeFilter;
