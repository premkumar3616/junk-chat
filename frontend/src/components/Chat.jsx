import { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import '../styles/chat.css';
import {
  FaPhone, FaPaperPlane, FaUser, FaSearch, FaPlus, FaArrowLeft,
} from 'react-icons/fa';

const API_BASE_URL = 'http://localhost:8080';

function Chat() {
  const [contacts, setContacts] = useState([]);
  const [selectedContact, setSelectedContact] = useState(null);
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [error, setError] = useState('');
  const [addContactUsername, setAddContactUsername] = useState('');
  const [addContactMessage, setAddContactMessage] = useState('');
  const [userId, setUserId] = useState(null);
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);
  const stompClient = useRef(null);
  const messagesEndRef = useRef(null);

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth <= 768);
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages]);

  const fetchUserProfile = async () => {
    try {
      const res = await axios.get(`${API_BASE_URL}/api/profile`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
      });
      setUserId(res.data.id);
    } catch (err) {
      setError('Failed to fetch user profile');
    }
  };

  const fetchContacts = async () => {
    try {
      const res = await axios.get(
        `${API_BASE_URL}/api/contacts${searchQuery ? `?search=${searchQuery}` : ''}`,
        { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } }
      );
      setContacts(res.data);
    } catch (err) {
      setError(err.response?.data || 'Failed to fetch contacts');
    }
  };

  const fetchMessages = async () => {
    if (!selectedContact) return;
    try {
      const res = await axios.get(`${API_BASE_URL}/api/messages/${selectedContact.id}`, {
        headers: { Authorization: `Bearer ${localStorage.getItem('token')}` },
      });
      setMessages(res.data);
    } catch (err) {
      setError(err.response?.data || 'Failed to fetch messages');
    }
  };

  const connectWebSocket = () => {
    if (!userId || !selectedContact) return;

    stompClient.current = new Client({
      webSocketFactory: () => new SockJS(`${API_BASE_URL}/ws`),
      connectHeaders: {
        Authorization: `Bearer ${localStorage.getItem('token')}`,
      },
      onConnect: () => {
        stompClient.current.subscribe(
          `/topic/messages/${userId}/${selectedContact.id}`,
          (message) => {
            const newMsg = JSON.parse(message.body);
            setMessages((prev) => (
              prev.find((msg) => msg.id === newMsg.id)
                ? prev
                : [...prev, newMsg]
            ));
          }
        );
      },
    });

    stompClient.current.activate();
  };

  const disconnectWebSocket = () => {
    if (stompClient.current) stompClient.current.deactivate();
  };

  useEffect(() => {
    fetchUserProfile();
    fetchContacts();
    return disconnectWebSocket;
  }, [searchQuery]);

  useEffect(() => {
    if (selectedContact && userId) {
      fetchMessages();
      connectWebSocket();
    }
    return disconnectWebSocket;
  }, [selectedContact, userId]);

  const handleAddContact = async (e) => {
    e.preventDefault();
    if (!addContactUsername.trim()) return;
    try {
      const res = await axios.post(
        `${API_BASE_URL}/api/contacts`,
        { username: addContactUsername },
        { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } }
      );
      setAddContactMessage(res.data);
      setAddContactUsername('');
      fetchContacts();
    } catch (err) {
      setAddContactMessage(err.response?.data || 'Failed to add contact');
    }
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
    } catch (err) {
      setError(err.response?.data || 'Failed to send message');
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage(e);
    }
  };

  const handleCall = () => {
    alert(`Calling ${selectedContact?.username}`);
  };

  const handleBack = () => {
    setSelectedContact(null);
  };

  const handleContactClick = (contact) => {
    setSelectedContact(contact);
  };

  const filteredContacts = contacts.filter((c) =>
    c.username.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="chat-container">
      <div className={`sidebar ${isMobile && selectedContact ? 'hidden' : ''}`}>
        <div className="sidebar-header">
          <Link to="/profile" className="profile-link">
            <FaUser style={{ marginRight: '8px' }} /> Edit Profile
          </Link>
        </div>
        <h2>Chats</h2>
        {error && <p className="error">{error}</p>}
        {addContactMessage && (
          <p className="message" style={{ color: addContactMessage.includes('success') ? 'green' : 'red' }}>
            {addContactMessage}
          </p>
        )}
        <div className="add-contact">
          <input
            type="text"
            value={addContactUsername}
            onChange={(e) => setAddContactUsername(e.target.value)}
            placeholder="Enter username to add..."
          />
          <button onClick={handleAddContact} className="btn btn-add"><FaPlus /></button>
        </div>
        <div className="search-bar">
          <FaSearch className="search-icon" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search contacts..."
          />
        </div>
        <ul>
          {filteredContacts.length > 0 ? (
            filteredContacts.map((contact) => (
              <li
                key={contact.id}
                className={`contact-item ${selectedContact?.id === contact.id ? 'selected' : ''}`}
                onClick={() => handleContactClick(contact)}
              >
                {contact.username}
              </li>
            ))
          ) : (
            <li className="no-contacts">No contacts found.</li>
          )}
        </ul>
      </div>

      <div className={`chat-area ${selectedContact ? 'active' : ''}`}>
        {selectedContact ? (
          <>
            <div className="chat-header">
              <div className="chat-header-left">
                {isMobile && (
                  <FaArrowLeft className="back-button" onClick={handleBack} />
                )}
                <h2>{selectedContact.username}</h2>
              </div>
              <button onClick={handleCall} className="btn btn-call">
                <FaPhone />
              </button>
            </div>
            <div className="messages">
              {messages.map((msg) => (
                <div
                  key={msg.id}
                  className={`message ${msg.sender.id === selectedContact.id ? 'received' : 'sent'}`}
                >
                  <span>{msg.content}</span>
                </div>
              ))}
              <div ref={messagesEndRef} />
            </div>
            <div className="message-input">
              <input
                type="text"
                value={newMessage}
                onChange={(e) => setNewMessage(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="Type a message..."
              />
              <button onClick={handleSendMessage} className="btn btn-send">
                <FaPaperPlane />
              </button>
            </div>
          </>
        ) : (
          <div className="no-chat">
            <p>Select a contact to start chatting</p>
          </div>
        )}
      </div>
    </div>
  );
}

export default Chat;