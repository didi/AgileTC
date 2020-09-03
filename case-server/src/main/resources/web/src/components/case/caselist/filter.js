/* eslint-disable */
import React from 'react';
import {
  Row,
  Button,
  Col,
  Select, Input,
  DatePicker
} from 'antd';
import './index.scss';
import moment from 'moment';
moment.locale('zh-cn');
const { RangePicker } = DatePicker;
const Option = Select.Option;

class Filter extends React.Component {
  static propTypes = {

  }
  constructor(props) {
    super(props);
    this.state = {
      choiseDate: [],
      iterationList: [], // 需求列表
      iterationFilter: [],
      createrFilter: [],
      current: 1,
      nameFilter: ''


    };
  }
  //   componentDidMount() {

  //   }
  nameFiltersInput = e => {
    this.setState({ nameFilter: e.target.value });
  }
  onDataChange = (value, dateString) => {
    this.setState({ choiseDate: dateString });
  }
  unDoFilter = () => {
    this.setState(
      {
        iterationFilter: '',
        nameFilter: '',
        choiseDate: [],
        createrFilter: '',
        current: 1
      },
      () => {
        const { current, nameFilter, createrFilter, iterationFilter, choiseDate } = this.state;
        this.props.getCaseList(current, nameFilter, createrFilter, iterationFilter, choiseDate);
      }
    );
  }
  doFilter = () => {
    this.setState({ current: 1 }, () => {

      const { current, nameFilter, createrFilter, iterationFilter, choiseDate } = this.state;
      this.props.getCaseList(current, nameFilter, createrFilter, iterationFilter, choiseDate);
    });
  }
  createPropleFilter = value => {
    this.setState({ createrFilter: value });
  }
  render() {
    const { choiseDate } = this.state;
    const { productMember } = this.props;
    return (
      <div className="filter-box m-b-10">
        <Row>
          <Col span={6} className="m-b-10">
            <div className="filter-item">
              <Input
                placeholder="用例集名称"
                style={{ width: '100%' }}
                onChange={this.nameFiltersInput}
                value={this.state.nameFilter}
              />
            </div>
          </Col>
          <Col span={6} className="m-b-10">
            <div className="filter-item">
              <Select
                style={{ width: '100%' }}
                placeholder="创建人"
                allowClear
                onChange={this.createPropleFilter}
                value={this.state.createrFilter ? this.state.createrFilter : undefined}
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
          <Col span={6} className="m-b-10">
            <div className="filter-item">
              <Select
                style={{ width: '100%' }}
                placeholder="所属需求"
                onChange={this.selectFilters}
                value={this.state.iterationFilter ? this.state.iterationFilter : undefined}
              >
                {this.state.iterationList.map((item, index) => {
                  return (
                    <Option key={item.id} value={item.id}>
                      {item.name}
                    </Option>
                  );
                })}
              </Select>
            </div>
          </Col>
          <Col span={6} className="m-b-10">
            <div className="filter-item">
              <RangePicker
                value={
                  choiseDate[0]
                    ? [moment(choiseDate[0], 'YYYY-MM-DD'), moment(choiseDate[1], 'YYYY-MM-DD')]
                    : null
                }
                format={'YYYY-MM-DD'}
                placeholder={['开始时间', '结束时间']}
                onChange={this.onDataChange}
              />
            </div>
          </Col>
        </Row>
        <Row style={{ marginTop: '10px' }}>
          <Col xs={12} offset={12} className="text-right">
            <Button className="m-r-10" onClick={this.unDoFilter}>
              重置
            </Button>
            <Button type="primary" onClick={this.doFilter}>
              筛选
            </Button>
          </Col>
        </Row>
      </div>
    );
  }
}
export default Filter;
