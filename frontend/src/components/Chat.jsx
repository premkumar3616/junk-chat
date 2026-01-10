import { useState, useEffect, useRef } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import axios from 'axios';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import '../styles/chat.css';
import { FaPhone, FaPaperPlane, FaUser, FaSearch, FaTrash, FaMoon, FaComment } from 'react-icons/fa';

const API_BASE_URL = import.meta.env.VITE_API_URL;

function Chat() {
  const [users, setUsers] = useState([]);
  const [selectedContact, setSelectedContact] = useState(null);
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [error, setError] = useState('');
  const [userId, setUserId] = useState(null);
  const [isDarkTheme, setIsDarkTheme] = useState(false);
  const [unreadCounts, setUnreadCounts] = useState({});
  const [showProfilePopup, setShowProfilePopup] = useState(false);
  const [popupContact, setPopupContact] = useState(null);
  const [showDeletePopup, setShowDeletePopup] = useState(false);
  const [contactToDelete, setContactToDelete] = useState(null);
  const stompClient = useRef(null);
  const messagesEndRef = useRef(null);
  const [isMobileView, setIsMobileView] = useState(window.innerWidth <= 768);
  const [showChatOnMobile, setShowChatOnMobile] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    document.body.classList.toggle('dark-theme', isDarkTheme);
  }, [isDarkTheme]);

  useEffect(() => {
    const handleResize = () => {
      const mobile = window.innerWidth <= 768;
      setIsMobileView(mobile);
      if (!mobile) {
        setShowChatOnMobile(false); // show both on desktop
      }
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  useEffect(() => {
    // Show login successful toast if coming from login page
    if (location.state?.fromLogin) {
      toast.success('Login successful!', {
        position: 'top-right',
        autoClose: 3000,
        hideProgressBar: false,
        closeOnClick: true,
        pauseOnHover: true,
        draggable: true,
      });
      // Clear the navigation state to prevent toast on subsequent navigations
      navigate(location.pathname, { replace: true, state: {} });
    }
  }, [location, navigate]);

  async function fetchUserProfile() {
    try {
      const response = await axios.get(`${API_BASE_URL}/api/profile`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      setUserId(response.data.id);
    } catch (error) {
      setError('Failed to fetch user profile: ' + error);
      toast.error('Failed to fetch user profile: ' + (error.response?.data || error.message), {
        position: 'top-right',
        autoClose: 3000,
        hideProgressBar: false,
        closeOnClick: true,
        pauseOnHover: true,
        draggable: true,
      });
    }
  }

  async function fetchUsers() {
    try {
      const response = await axios.get(`${API_BASE_URL}/api/users/search`, {
        params: { query: searchQuery },
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      const sortedUsers = response.data.sort((a, b) => {
        const timeA = a.lastMessageTime ? new Date(a.lastMessageTime) : new Date(0);
        const timeB = b.lastMessageTime ? new Date(b.lastMessageTime) : new Date(0);
        return timeB - timeA;
      });
      setUsers(sortedUsers);
      setUnreadCounts(prev => {
        const newCounts = { ...prev };
        sortedUsers.forEach(user => {
          if (user.unreadCount > 0) {
            newCounts[user.id] = user.unreadCount;
            console.log(`Set unread count for user ${user.id}: ${user.unreadCount}`);
          } else {
            delete newCounts[user.id];
          }
        });
        return newCounts;
      });
      setError('');
    } catch (error) {
      setError(error.response?.data || 'Failed to fetch users');
      toast.error('Failed to fetch users: ' + (error.response?.data || error.message), {
        position: 'top-right',
        autoClose: 3000,
        hideProgressBar: false,
        closeOnClick: true,
        pauseOnHover: true,
        draggable: true,
      });
    }
  }

  async function fetchMessages() {
    if (!selectedContact) return;
    try {
      const response = await axios.get(`${API_BASE_URL}/api/messages/${selectedContact.id}`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
      });
      setMessages(response.data);
      setError('');
      // Mark messages as read
      await axios.post(
        `${API_BASE_URL}/api/messages/mark-read/${selectedContact.id}`,
        {},
        { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } }
      );
      setUnreadCounts(prev => {
        const newCounts = { ...prev };
        delete newCounts[selectedContact.id];
        return newCounts;
      });
    } catch (error) {
      setError(error.response?.data || 'Failed to fetch messages');
      toast.error('Failed to fetch messages: ' + (error.response?.data || error.message), {
        position: 'top-right',
        autoClose: 3000,
        hideProgressBar: false,
        closeOnClick: true,
        pauseOnHover: true,
        draggable: true,
      });
    }
  }

  const connectWebSocket = () => {
    if (!userId) return;

    stompClient.current = new Client({
      webSocketFactory: () => new SockJS(`${API_BASE_URL}/ws`),
      connectHeaders: {
        Authorization: `Bearer ${localStorage.getItem('token')}`
      },
      onConnect: () => {
        // Subscribe to contact updates
        stompClient.current.subscribe(
          `/topic/contacts/${userId}`,
          (message) => {
            const newContact = JSON.parse(message.body);
            setUsers(prev => {
              const existingUserIndex = prev.findIndex(user => user.id === newContact.id);
              if (existingUserIndex === -1) {
                return [newContact, ...prev];
              }
              const updatedUsers = [...prev];
              updatedUsers.splice(existingUserIndex, 1);
              return [newContact, ...updatedUsers].sort((a, b) => {
                const timeA = a.lastMessageTime ? new Date(a.lastMessageTime) : new Date(0);
                const timeB = b.lastMessageTime ? new Date(b.lastMessageTime) : new Date(0);
                return timeB - timeA;
              });
            });
            if (newContact.unreadCount > 0) {
              setUnreadCounts(prev => ({
                ...prev,
                [newContact.id]: newContact.unreadCount
              }));
            }
          }
        );

        // Subscribe to contact removals
        stompClient.current.subscribe(
          `/topic/contacts/remove/${userId}`,
          (message) => {
            const removedContact = JSON.parse(message.body);
            console.log('Received contact removal on /topic/contacts/remove/', removedContact);
            setUsers(prev => prev.filter(user => user.id !== removedContact.id));
            if (selectedContact && selectedContact.id === removedContact.id) {
              setSelectedContact(null);
              setMessages([]);
            }
          }
        );

        // Subscribe to contact list updates
        stompClient.current.subscribe(
          `/topic/messages/${userId}`,
          (message) => {
            const updatedContact = JSON.parse(message.body);
            if (!updatedContact.id) {
              return;
            }
            setUsers(prev => {
              const existingIndex = prev.findIndex(u => u.id === updatedContact.id);
              const newContact = {
                id: updatedContact.id,
                username: updatedContact.username,
                profilePic: updatedContact.profilePic,
                lastMessageContent: updatedContact.lastMessageContent,
                lastMessageTime: updatedContact.lastMessageTime,
                unreadCount: updatedContact.unreadCount || 0
              };
              let updatedUsers;
              if (existingIndex === -1) {
                updatedUsers = [newContact, ...prev];
              } else {
                updatedUsers = [...prev];
                updatedUsers.splice(existingIndex, 1);
                updatedUsers = [newContact, ...updatedUsers];
              }
              return updatedUsers.sort((a, b) => {
                const timeA = a.lastMessageTime ? new Date(a.lastMessageTime) : new Date(0);
                const timeB = b.lastMessageTime ? new Date(b.lastMessageTime) : new Date(0);
                return timeB - timeA;
              });
            });
            if (selectedContact?.id !== updatedContact.id && updatedContact.unreadCount > 0) {
              setUnreadCounts(prev => {
                const newCounts = { ...prev, [updatedContact.id]: updatedContact.unreadCount };
                console.log(`Updated unread count for user ${updatedContact.id}: ${updatedContact.unreadCount}`);
                return newCounts;
              });
            }
            // If the updated contact is the selected contact, fetch messages to ensure the chat box updates
            if (selectedContact?.id === updatedContact.id) {
              fetchMessages();
            }
          }
        );

        // Subscribe to messages for the selected contact
        if (selectedContact) {
          stompClient.current.subscribe(
            `/topic/messages/${userId}/${selectedContact.id}`,
            (message) => {
              const newMessage = JSON.parse(message.body);
              console.log('Received message on /topic/messages/', newMessage);
              if (!newMessage.hiddenForUserIds || !newMessage.hiddenForUserIds.includes(userId)) {
                setMessages((prev) => {
                  if (!prev.find((msg) => msg.id === newMessage.id)) {
                    return [...prev, newMessage];
                  }
                  return prev;
                });
              }
            }
          );
        }
      },
      onStompError: (error) => {
        setError('WebSocket connection failed: ' + error);
        toast.error('WebSocket connection failed: ' + error, {
          position: 'top-right',
          autoClose: 3000,
          hideProgressBar: false,
          closeOnClick: true,
          pauseOnHover: true,
          draggable: true,
        });
      }
    });

    stompClient.current.activate();
  };

  const disconnectWebSocket = () => {
    if (stompClient.current) {
      stompClient.current.deactivate();
      console.log('WebSocket disconnected');
    }
  };

  const scrollToBottom = () => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  };

  const handleRemoveContact = async () => {
    if (!selectedContact) return;
    setContactToDelete(selectedContact);
    setShowDeletePopup(true);
  };

  const confirmDeleteContact = async () => {
    if (!contactToDelete) return;
    try {
      await axios.delete(`${API_BASE_URL}/api/contacts`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
        data: { username: contactToDelete.username }
      });
      setError('');
      toast.success(`Contact ${contactToDelete.username} removed successfully`, {
        position: 'top-right',
        autoClose: 3000,
        hideProgressBar: false,
        closeOnClick: true,
        pauseOnHover: true,
        draggable: true,
      });
      setShowDeletePopup(false);
      setContactToDelete(null);
    } catch (error) {
      setError(error.response?.data || 'Failed to remove contact');
      toast.error('Failed to remove contact: ' + (error.response?.data || error.message), {
        position: 'top-right',
        autoClose: 3000,
        hideProgressBar: false,
        closeOnClick: true,
        pauseOnHover: true,
        draggable: true,
      });
      setShowDeletePopup(false);
      setContactToDelete(null);
    }
  };

  const cancelDeleteContact = () => {
    setShowDeletePopup(false);
    setContactToDelete(null);
  };

  const handleSendMessage = async (e) => {
    e.preventDefault();
    if (!newMessage.trim() || !selectedContact) return;
    try {
      await axios.post(
        `${API_BASE_URL}/api/messages`,
        { recipientId: selectedContact.id, content: newMessage },
        { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } }
      );
      setNewMessage('');
      // Fetch messages immediately after sending to ensure the sent message appears
      await fetchMessages();
    } catch (error) {
      setError(error.response?.data || 'Failed to send message');
      toast.error('Failed to send message: ' + (error.response?.data || error.message), {
        position: 'top-right',
        autoClose: 3000,
        hideProgressBar: false,
        closeOnClick: true,
        pauseOnHover: true,
        draggable: true,
      });
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage(e);
    }
  };

  const handleCall = () => {
    alert(`Initiating call with ${selectedContact?.username || popupContact?.username}`);
  };

  const toggleTheme = () => {
    setIsDarkTheme(!isDarkTheme);
  };

  const handleProfilePicClick = (user) => {
    setPopupContact(user);
    setShowProfilePopup(true);
  };

  const closeProfilePopup = () => {
    setShowProfilePopup(false);
    setPopupContact(null);
  };

  const handleMessageClick = () => {
    if (popupContact) {
      setSelectedContact(popupContact);
      setShowProfilePopup(false);
    }
  };

  const formatTimestamp = (sentAt) => {
    if (!sentAt) return '';
    const utcString = sentAt.endsWith('Z') ? sentAt : `${sentAt}Z`;
    const date = new Date(utcString);
    return date.toLocaleString('en-IN', {
      timeZone: 'Asia/Kolkata',
      month: 'short',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: true
    });
  };

  useEffect(() => {
    fetchUserProfile();
    fetchUsers();
    return () => disconnectWebSocket();
  }, [searchQuery]);

  useEffect(() => {
    if (userId) connectWebSocket();
    if (selectedContact && userId) fetchMessages();
    return () => disconnectWebSocket();
  }, [selectedContact, userId]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/');
    }
  }, []);

  return (
    <div className={`chat-container ${isDarkTheme ? 'dark-theme' : ''}`}>
      <div className={`sidebar ${isDarkTheme ? 'dark-theme' : ''} ${isMobileView && showChatOnMobile ? 'hidden' : ''}`}>
        <div className={`sidebar-header ${isDarkTheme ? 'dark-theme' : ''}`}>
          <Link to="/profile" className="profile-link">
            <FaUser style={{ marginRight: '8px', color: isDarkTheme ? 'white' : 'black' }} />
          </Link>
          <h2>Chats</h2>
          <FaMoon 
            onClick={toggleTheme} 
            style={{ 
              cursor: 'pointer', 
              marginLeft: '8px', 
              color: isDarkTheme ? 'white' : 'black' 
            }} 
          />
        </div>
        {error && <p className="error" style={{ color: 'red', padding: '10px' }}>{error}</p>}
        <div className={`search-bar ${isDarkTheme ? 'dark-theme' : ''}`}>
          <FaSearch className="search-icon" style={{ color: isDarkTheme ? 'white' : 'black' }} />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search users..."
          />
        </div>
        <ul>
          {users.length > 0 ? (
            users.map((user) => (
              <li
                key={user.id}
                className={`contact-item ${isDarkTheme ? 'dark-theme' : ''} ${selectedContact?.id === user.id ? 'selected' : ''} ${unreadCounts[user.id] > 0 ? 'unread' : ''}`}
                onClick={() => {
                  setSelectedContact(user);
                  if (isMobileView) setShowChatOnMobile(true);
                }}
              >
                <img 
                  src={user.profilePic || 'https://via.placeholder.com/40'} 
                  alt="Profile" 
                  className="contact-pic" 
                  onClick={(e) => {
                    e.stopPropagation();
                    handleProfilePicClick(user);
                  }}
                  style={{ cursor: 'pointer' }}
                />
                <div className="contact-info">
                  <div className="contact-name-wrapper">
                    <span className="contact-name">{user.username}</span>
                    {unreadCounts[user.id] > 0 && (
                      <span className={`unread-count ${isDarkTheme ? 'dark-theme' : ''}`}>
                        {unreadCounts[user.id]}
                      </span>
                    )}
                  </div>
                  <div className="last-message-wrapper">
                    <span className={`last-message ${isDarkTheme ? 'dark-theme' : ''}`}>
                      {user.lastMessageContent || 'No messages yet'}
                    </span>
                    {user.lastMessageTime && (
                      <span className={`last-message-time ${isDarkTheme ? 'dark-theme' : ''}`}>
                        {formatTimestamp(user.lastMessageTime)}
                      </span>
                    )}
                  </div>
                </div>
              </li>
            ))
          ) : (
            <li className={`no-contacts ${isDarkTheme ? 'dark-theme' : ''}`}>No users found.</li>
          )}
        </ul>
      </div>
  
      <div className={`chat-area ${isMobileView && showChatOnMobile ? 'active' : ''}`}>
        {selectedContact ? (
          <>
            <div className={`chat-header ${isDarkTheme ? 'dark-theme' : ''}`}>
              <div className="chat-header-left">
                {isMobileView && (
                  <button className="back-button" onClick={() => setShowChatOnMobile(false)}>
                    ←
                  </button>
                )}
                <img 
                  src={selectedContact.profilePic || 'https://via.placeholder.com/40'} 
                  alt="Profile" 
                  className="contact-pic" 
                  onClick={() => handleProfilePicClick(selectedContact)}
                  style={{ cursor: 'pointer' }}
                />
                <h2>{selectedContact.username}</h2>
              </div>
              <div className="chat-header-buttons">
                <button onClick={handleCall} className={`btn btn-call ${isDarkTheme ? 'dark-theme' : ''}`} title="Call">
                  <FaPhone />
                </button>
                <button 
                  onClick={handleRemoveContact} 
                  className={`btn btn-remove ${isDarkTheme ? 'dark-theme' : ''}`} 
                  title="Remove Contact" 
                  style={{ marginLeft: '10px' }}
                >
                  <FaTrash />
                </button>
              </div>
            </div>

            <div className={`message-notice ${isDarkTheme ? 'dark-theme' : ''}`}>
              <p>Messages are automatically deleted after 24 hours.</p>
            </div>

            <div className={`messages ${isDarkTheme ? 'dark-theme' : ''}`}>
              {messages.map((msg) => (
                <div
                  key={msg.id}
                  className={`message ${msg.sender.id === selectedContact.id ? 'received' : 'sent'} ${isDarkTheme ? 'dark-theme' : ''}`}
                >
                  <span className="message-content">{msg.content}</span>
                  <span className={`message-timestamp ${isDarkTheme ? 'dark-theme' : ''}`}>
                    {formatTimestamp(msg.sentAt)}
                  </span>
                </div>
              ))}
              <div ref={messagesEndRef} />
            </div>

            <div className={`message-input ${isDarkTheme ? 'dark-theme' : ''}`}>
              <input
                type="text"
                value={newMessage}
                onChange={(e) => setNewMessage(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="Type a message..."
              />
              <button onClick={handleSendMessage} className={`btn btn-send ${isDarkTheme ? 'dark-theme' : ''}`}>
                <FaPaperPlane />
              </button>
            </div>
          </>
        ) : (
          <div className={`no-chat ${isDarkTheme ? 'dark-theme' : ''}`}>
            <p>Select a user to start chatting</p>
            <p>Messages are automatically deleted after 24 hours.</p>
          </div>
        )}
      </div>

      {showProfilePopup && popupContact && (
        <div className={`profile-popup ${isDarkTheme ? 'dark-theme' : ''}`}>
          <div className="profile-popup-content">
            <button 
              className="close-popup" 
              onClick={closeProfilePopup}
              style={{ 
                position: 'absolute', 
                top: '10px', 
                right: '10px', 
                background: 'transparent', 
                border: 'none', 
                fontSize: '20px', 
                cursor: 'pointer',
                color: isDarkTheme ? 'white' : 'black'
              }}
            >
              ×
            </button>
            <img 
              src={popupContact.profilePic || 'https://via.placeholder.com/150'} 
              alt="Profile" 
              className="popup-profile-pic"
              style={{ width: '150px', height: '150px', borderRadius: '50%', marginBottom: '10px' }}
            />
            <h3>{popupContact.username}</h3>
            <div className="popup-actions">
              <button 
                onClick={handleCall} 
                className={`btn btn-call ${isDarkTheme ? 'dark-theme' : ''}`}
                title="Call"
              >
                <FaPhone />
              </button>
              <button 
                onClick={handleMessageClick} 
                className={`btn btn-message ${isDarkTheme ? 'dark-theme' : ''}`}
                title="Message"
                style={{ marginLeft: '10px' }}
              >
                <FaComment />
              </button>
            </div>
          </div>
        </div>
      )}

      {showDeletePopup && contactToDelete && (
        <div className={`profile-popup ${isDarkTheme ? 'dark-theme' : ''}`}>
          <div className="profile-popup-content">
            <button 
              className="close-popup" 
              onClick={cancelDeleteContact}
              style={{ 
                position: 'absolute', 
                top: '10px', 
                right: '10px', 
                background: 'transparent', 
                border: 'none', 
                fontSize: '20px', 
                cursor: 'pointer',
                color: isDarkTheme ? 'white' : 'black'
              }}
            >
              ×
            </button>
            <h3>Remove {contactToDelete.username}?</h3>
            <p style={{ fontSize: '14px', color: isDarkTheme ? '#a0a0a0' : '#667781', margin: '10px 0' }}>
              This will clear your chat with {contactToDelete.username}.
            </p>
            <div className="popup-actions">
              <button 
                onClick={cancelDeleteContact} 
                className={`btn btn-message ${isDarkTheme ? 'dark-theme' : ''}`}
                style={{ backgroundColor: isDarkTheme ? '#2d2d2d' : '#f0f2f5', color: isDarkTheme ? '#e0e0e0' : '#000' }}
              >
                Cancel
              </button>
              <button 
                onClick={confirmDeleteContact} 
                className={`btn btn-remove ${isDarkTheme ? 'dark-theme' : ''}`}
                style={{ backgroundColor: '#ff6b6b', color: '#ffffff', marginLeft: '10px' }}
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}

      <ToastContainer />
    </div>
  );
}

export default Chat;