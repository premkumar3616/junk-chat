import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import '../styles/login.css';
const API_BASE_URL = import.meta.env.VITE_API_URL;

function Profile() {
  const [userData, setUserData] = useState({ username: '', email: '', profilePic: '' });
  const [previewPic, setPreviewPic] = useState(null);
  const navigate = useNavigate();

  async function fetchProfile() {
    try {
      const response = await axios.get(`${API_BASE_URL}/api/profile`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      setUserData({ username: response.data.username, email: response.data.email, profilePic: response.data.profilePic });
      setPreviewPic(response.data.profilePic || 'https://via.placeholder.com/150');
    } catch (error) {
      
      toast.error('Failed to fetch profile: ' + (error.response?.data || error.message), {
        position: 'top-right',
        autoClose: 3000,
        hideProgressBar: false,
        closeOnClick: true,
        pauseOnHover: true,
        draggable: true,
      });
    }
  }

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      if (file.size > 5 * 1024 * 1024) {
        toast.error('Image size must be less than 5MB', {
          position: 'top-right',
          autoClose: 3000,
          hideProgressBar: false,
          closeOnClick: true,
          pauseOnHover: true,
          draggable: true,
        });
        e.target.value = '';
        return;
      }
      const reader = new FileReader();
      reader.onloadend = () => {
        setUserData({ ...userData, profilePic: reader.result });
        setPreviewPic(reader.result);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleChange = (e) => {
    setUserData({ ...userData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const updatePayload = {
        username: userData.username || undefined,
        email: userData.email || undefined,
        profilePic: userData.profilePic || undefined
      };
      await axios.put(`${API_BASE_URL}/api/profile`, updatePayload, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      toast.success('Profile updated successfully', {
        position: 'top-right',
        autoClose: 3000,
        hideProgressBar: false,
        closeOnClick: true,
        pauseOnHover: true,
        draggable: true,
      });
    } catch (error) {
      
      toast.error('Failed to update profile: ' + (error.response?.data || error.message), {
        position: 'top-right',
        autoClose: 3000,
        hideProgressBar: false,
        closeOnClick: true,
        pauseOnHover: true,
        draggable: true,
      });
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    navigate('/', { state: { fromLogout: true } });
  };

  useEffect(() => {
    fetchProfile();
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/', { state: { fromLogout: true } });
    }
  }, []);

  return (
    <div className="auth-container">
      <div className="auth-form">
        <h2>Edit Profile</h2>
        <div className="profile-pic-section">
          <img
            src={previewPic}
            alt="Profile"
            className="profile-pic-preview"
          />
          <label htmlFor="profilePic" className="profile-pic-label">
            Change Profile Photo
            <input
              id="profilePic"
              type="file"
              accept="image/*"
              onChange={handleFileChange}
              style={{ display: 'none' }}
            />
          </label>
        </div>
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
      <ToastContainer />
    </div>
  );
}

export default Profile;