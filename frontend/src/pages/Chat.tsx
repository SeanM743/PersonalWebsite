import React, { useState, useEffect, useRef } from 'react';
import { apiService } from '../services/apiService';
import { useNotification } from '../contexts/NotificationContext';
import LoadingSpinner from '../components/UI/LoadingSpinner';
import { Send, Bot, User } from 'lucide-react';

interface ChatMessage {
  id: string;
  message: string;
  response: string;
  timestamp: string;
  isUser: boolean;
}

const Chat: React.FC = () => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputMessage, setInputMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const { error } = useNotification();

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  useEffect(() => {
    loadChatHistory();
  }, []);

  const loadChatHistory = async () => {
    try {
      setIsLoading(true);
      const response = await apiService.getChatHistory();
      if (response.success && response.data) {
        // Transform the chat history into message format
        const chatMessages: ChatMessage[] = [];
        response.data.forEach((chat: any, index: number) => {
          chatMessages.push({
            id: `user-${index}`,
            message: chat.message,
            response: '',
            timestamp: chat.timestamp,
            isUser: true,
          });
          chatMessages.push({
            id: `bot-${index}`,
            message: chat.response,
            response: '',
            timestamp: chat.timestamp,
            isUser: false,
          });
        });
        setMessages(chatMessages);
      }
    } catch (err: any) {
      error('Failed to load chat history', err.message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!inputMessage.trim() || isSending) return;

    const userMessage = inputMessage.trim();
    setInputMessage('');
    setIsSending(true);

    // Add user message immediately
    const userMessageObj: ChatMessage = {
      id: `user-${Date.now()}`,
      message: userMessage,
      response: '',
      timestamp: new Date().toISOString(),
      isUser: true,
    };
    setMessages(prev => [...prev, userMessageObj]);

    try {
      const response = await apiService.sendChatMessage(userMessage);
      
      if (response.success) {
        // Add bot response
        const botMessageObj: ChatMessage = {
          id: `bot-${Date.now()}`,
          message: response.data.response || 'I received your message but had no response.',
          response: '',
          timestamp: new Date().toISOString(),
          isUser: false,
        };
        setMessages(prev => [...prev, botMessageObj]);
      } else {
        throw new Error(response.error || 'Failed to get response');
      }
    } catch (err: any) {
      error('Failed to send message', err.message);
      
      // Add error message
      const errorMessageObj: ChatMessage = {
        id: `error-${Date.now()}`,
        message: 'Sorry, I encountered an error processing your message. Please try again.',
        response: '',
        timestamp: new Date().toISOString(),
        isUser: false,
      };
      setMessages(prev => [...prev, errorMessageObj]);
    } finally {
      setIsSending(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <LoadingSpinner size="large" />
      </div>
    );
  }

  return (
    <div className="flex flex-col h-[calc(100vh-12rem)]">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">AI Chat Assistant</h1>
        <div className="flex items-center space-x-2 text-sm text-gray-500">
          <Bot className="h-4 w-4" />
          <span>Powered by Gemini AI</span>
        </div>
      </div>

      {/* Chat Messages */}
      <div className="flex-1 bg-white rounded-lg shadow-sm border border-gray-200 flex flex-col">
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {messages.length === 0 ? (
            <div className="text-center py-12">
              <Bot className="mx-auto h-12 w-12 text-gray-400" />
              <h3 className="mt-2 text-sm font-medium text-gray-900">Start a conversation</h3>
              <p className="mt-1 text-sm text-gray-500">
                Ask me anything about your portfolio, calendar, or content!
              </p>
            </div>
          ) : (
            messages.map((message) => (
              <div
                key={message.id}
                className={`flex ${message.isUser ? 'justify-end' : 'justify-start'}`}
              >
                <div
                  className={`max-w-xs lg:max-w-md px-4 py-2 rounded-lg ${
                    message.isUser
                      ? 'bg-blue-600 text-white'
                      : 'bg-gray-100 text-gray-900'
                  }`}
                >
                  <div className="flex items-start space-x-2">
                    {!message.isUser && (
                      <Bot className="h-4 w-4 mt-0.5 flex-shrink-0" />
                    )}
                    <div className="flex-1">
                      <p className="text-sm whitespace-pre-wrap">{message.message}</p>
                      <p className="text-xs opacity-75 mt-1">
                        {new Date(message.timestamp).toLocaleTimeString()}
                      </p>
                    </div>
                    {message.isUser && (
                      <User className="h-4 w-4 mt-0.5 flex-shrink-0" />
                    )}
                  </div>
                </div>
              </div>
            ))
          )}
          
          {isSending && (
            <div className="flex justify-start">
              <div className="max-w-xs lg:max-w-md px-4 py-2 rounded-lg bg-gray-100 text-gray-900">
                <div className="flex items-center space-x-2">
                  <Bot className="h-4 w-4" />
                  <LoadingSpinner size="small" />
                  <span className="text-sm">Thinking...</span>
                </div>
              </div>
            </div>
          )}
          
          <div ref={messagesEndRef} />
        </div>

        {/* Message Input */}
        <div className="border-t border-gray-200 p-4">
          <form onSubmit={handleSendMessage} className="flex space-x-2">
            <input
              type="text"
              value={inputMessage}
              onChange={(e) => setInputMessage(e.target.value)}
              placeholder="Type your message..."
              className="flex-1 input-field"
              disabled={isSending}
            />
            <button
              type="submit"
              disabled={!inputMessage.trim() || isSending}
              className="btn-primary flex items-center space-x-2"
            >
              <Send className="h-4 w-4" />
              <span className="hidden sm:inline">Send</span>
            </button>
          </form>
          
          <div className="mt-2 text-xs text-gray-500">
            Try asking: "What's my portfolio performance?" or "What events do I have today?"
          </div>
        </div>
      </div>
    </div>
  );
};

export default Chat;