import React, {Component} from 'react';
import {BrowserRouter as Router, Link, Route} from 'react-router-dom';
import reactLogo from './images/react.svg';
import playLogo from './images/play.svg';
import scalaLogo from './images/scala.png';

import IndexPage from './page/IndexPage';

import 'antd/dist/antd.css';
import './App.scss';

import {Breadcrumb, Icon, Layout, Menu} from 'antd';
// import Sidebar from 'react-sidebar';


// import Client from "./Client";
const { SubMenu } = Menu;
const { Header, Content, Footer, Sider } = Layout;

class App extends Component {
  constructor(props) {
    super(props);
    this.state = {
      title: 'Scala Play React Seed',
      collapsed: false,
    };
  }

  onCollapse = (collapsed) => {
    console.log(collapsed);
    this.setState({ collapsed });
  };

  toggle = () => {
    this.setState({
      collapsed: !this.state.collapsed,
    });
  };

  render() {
    return (
      <Router>
        <Layout style={{ background: '#fff'}} className="App">

          <Sider style={{background: '#fff'}}
                 collapsible
                 collapsed={this.state.collapsed}
                 onCollapse={this.onCollapse}>
            <div className="logo" />
            <Menu
              mode="inline"
              defaultSelectedKeys={['1']}
              defaultOpenKeys={['sub1']}
              style={{height: '100%'}}
            >
              <SubMenu key="sub1" title={<span><Icon type="user"/><span>subnav 1</span></span>}>
                <Menu.Item key="1">
                  <Link to="scala">
                    <img width="45" height="30" src={scalaLogo} alt="Scala Logo"/>
                  </Link>
                </Menu.Item>
                <Menu.Item key="2">
                  <Link to="play">
                    <img width="40" height="40" src={playLogo} alt="Play Framework Logo"/>
                  </Link>
                </Menu.Item>
                <Menu.Item key="3">
                  <Link to="react">
                    <img width="40" height="40" src={reactLogo} className="App-logo" alt="React Logo"/>
                  </Link>
                </Menu.Item>
                <Menu.Item key="4">option4</Menu.Item>
              </SubMenu>
              <SubMenu key="sub2" title={<span><Icon type="laptop"/><span>subnav 2</span></span>}>
                <Menu.Item key="5">option5</Menu.Item>
                <Menu.Item key="6">option6</Menu.Item>
                <Menu.Item key="7">option7</Menu.Item>
                <Menu.Item key="8">option8</Menu.Item>
              </SubMenu>
              <SubMenu key="sub3" title={<span><Icon type="notification"/><span>subnav 3</span></span>}>
                <Menu.Item key="9">option9</Menu.Item>
                <Menu.Item key="10">option10</Menu.Item>
                <Menu.Item key="11">option11</Menu.Item>
                <Menu.Item key="12">option12</Menu.Item>
              </SubMenu>
            </Menu>
          </Sider>
          <Layout>
            <Header style={{ background: '#fff', padding: 0 }}>
              <Icon
                className="trigger"
                type={this.state.collapsed ? 'menu-unfold' : 'menu-fold'}
                onClick={this.toggle}
              />
            </Header>
            <Content style={{margin: '0 16px', minHeight: 280}}>
              <Breadcrumb style={{margin: '16px 0'}}>
                <Breadcrumb.Item>Home</Breadcrumb.Item>
                <Breadcrumb.Item>List</Breadcrumb.Item>
                <Breadcrumb.Item>App</Breadcrumb.Item>
              </Breadcrumb>
              <div>
                <Route path="/:tech" component={IndexPage}/>
              </div>
            </Content>
            <Footer style={{textAlign: 'center'}}>
              Ant Design ©2016 Created by Ant UED
            </Footer>
          </Layout>
        </Layout>
      </Router>
    );
  }
}
export default App;
