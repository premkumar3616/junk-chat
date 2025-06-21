import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import '../styles/login.css';
function Profile() {
  const [userData, setUserData] = useState({ username: '', email: '' });
const navigate = useNavigate();
  async function fetchProfile() {
    try {
      const response = await axios.get('/api/profile', {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      setUserData(response.data);
    }
    catch (error) {
      console.error('Error Fetching Details : ', error);
    }
  }

  useEffect(() => {
    fetchProfile();
  }, []);
const handleChange = (e)=>{
  setUserData({...userData,[e.target.name]:e.target.value})
};
const handleSubmit = async (e)=>{
  e.preventDefault();
  try{
await axios.put('/api/profile',userData,{
  headers : { Authorization: `Bearer ${localStorage.getItem('token')}`}
});
alert('Profile updated successfully');
  }
  catch(error){
    console.error('error updating profile ',error);

  }
}
const handleLogout = () => {
    localStorage.removeItem('token');
    navigate('/');
  };

  return (
    <div className="auth-container">
      <div className="auth-form">
        <h2>Edit Profile</h2>
        <div>
          <div className="form-group">
            <label>Username</label>
            <input
              type="text"
              name="username"
              value={userData.username}
              onChange={handleChange}
            />
          </div>
          <div className="form-group">
            <label>Email</label>
            <input
              type="email"
              name="email"
              value={userData.email}
              onChange={handleChange}
            />
          </div>
          <div className="button-group">
  <button onClick={handleSubmit} className="btn">
    Update Profile
  </button>
  <button onClick={handleLogout} className="btn" style={{ backgroundColor: '#ff6b6b' }}>
    Logout
  </button>
</div>

        </div>
      </div>
    </div>
  )
}

export default Profile