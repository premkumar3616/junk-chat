import { Routes,Route } from 'react-router-dom'
import Login from './components/Login';
import Register from './components/Register.jsx';
import ForgotPassword from './components/ForgotPassword.jsx';
import Chat from './components/Chat.jsx';
import Profile from './components/Profile.jsx'
import ResetPassword from './components/ResetPassword.jsx';
function App() {


  return (
    <>
      <Routes>
        <Route path = "/" element = {<Login/>} />
        <Route path = "/register" element = {<Register/>} />
        <Route path = "/forgot-password" element = {<ForgotPassword/>} />
        <Route path = "/chat" element = {<Chat/>} />
        <Route path="/profile" element={<Profile />} />
        <Route path='/reset-password' element={<ResetPassword/>} />
      </Routes>
    </>
  )
}

export default App
