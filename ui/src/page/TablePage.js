import React, {Component} from 'react';
import {Table, Icon, Divider} from 'antd';
import './IndexPage.css';

const { Column, ColumnGroup } = Table;

class TablePage extends Component {
  state = {
    selectedRowKeys: [],
  };

  selectRow = (record) => {
    const selectedRowKeys = [...this.state.selectedRowKeys];
    if (selectedRowKeys.indexOf(record.key) >= 0) {
      selectedRowKeys.splice(selectedRowKeys.indexOf(record.key), 1);
    } else {
      selectedRowKeys.push(record.key);
    }
    this.setState({ selectedRowKeys });
  };

  onSelectedRowKeysChange = (selectedRowKeys) => {
    this.setState({ selectedRowKeys });
  }

  render() {
    const dataSource = [{
      key: '1',
      name: 'Mike',
      age: 32,
      address: '10 Downing Street'
    }, {
      key: '2',
      name: 'John',
      age: 42,
      address: '10 Downing Street'
    }, {
      key: '3',
      firstName: 'Joe',
      lastName: 'Black',
      age: 32,
      address: 'Sidney No. 1 Lake Park',
    }];

    const columns = [{
      title: 'Name',
      dataIndex: 'name',
      key: 'name',
    }, {
      title: 'Age',
      dataIndex: 'age',
      key: 'age',
    }, {
      title: 'Address',
      dataIndex: 'address',
      key: 'address',
    }, {
      title: 'Action',
      key: 'action',
      render: (text, record) => (
        <span>
      <a href="javascript:;">Action 一 {record.name}</a>
      <Divider type="vertical" />
      <a href="javascript:;">Delete</a>
      <Divider type="vertical" />
      <a href="javascript:;" className="ant-dropdown-link">
        More actions <Icon type="down" />
      </a>
    </span>
      ),
    }];

    const { selectedRowKeys } = this.state;

    const rowSelection = {
      selectedRowKeys,
      onChange: this.onSelectedRowKeysChange,
    };

    return (<Table
                  rowSelection={rowSelection}
                   dataSource={dataSource}
                   columns={columns}
                   onRow={(record) => ({
                     onClick: () => {
                       this.selectRow(record);
                     },
                   })}>
      <ColumnGroup title="Name">
        <Column
          title="First Name"
          dataIndex="firstName"
          key="firstName"
        />
        <Column
          title="Last Name"
          dataIndex="lastName"
          key="lastName"
        />
      </ColumnGroup>
      <Column
        title="Action"
        key="action"
        render={(text, record) => (
          <span>
          <a href="javascript:;">Action 一 {record.name}</a>
          <Divider type="vertical" />
          <a href="javascript:;">Delete</a>
          <Divider type="vertical" />
          <a href="javascript:;" className="ant-dropdown-link">
            More actions <Icon type="down" />
          </a>
        </span>
        )}
      />
    </Table>)
  }
}

export default TablePage;
