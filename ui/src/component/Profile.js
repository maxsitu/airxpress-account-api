import React, {Component} from 'react';
import PropTypes from 'prop-types';

import './Profile.scss';

class Profile extends Component {
  render() {
    const {id, profile} = this.props;
    return (
      <div className={`Profile Profile-${id}`}>
        <div className={`Profile__username Profile-${id}__username`}>
          <div>Name: </div> <span>{profile.username}</span>
        </div>
        <div className={`Profile__age Profile-${id}__age`}>
          <div>Age:  </div> <span>{profile.age}</span>
        </div>
      </div>);
  }
}

const profileShape = {
  username: PropTypes.string.isRequired,
  age:      PropTypes.number.isRequired
};

Profile.propTypes = {
  id: PropTypes.string.isRequired,
  profile: PropTypes.shape(profileShape).isRequired
};

export default Profile;
