import React, {Component} from 'react';
import Profile from '../component/Profile';

import './IndexPage.css';

class IndexPage extends Component {
  render() {
    const profile = {
      username: "situ.ma",
      age: 20
    };

    return (
      <div className={"index-page"}>
        <div>Path: {this.props.match.params.tech}</div>
        <Profile id={"situ.ma"} profile={profile}/>
      </div>
    );
  }
}

export default IndexPage;
