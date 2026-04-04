import React, { useState, useEffect } from 'react';
// import './Chatbot.css'; // for later

function Chatbot() {
    const [inputMessage, setInputMessage] = useState('');
    const [responseText, setResponseText] = useState('');
    const [isGenerating, setIsGenerating] = useState(false);
    const [error, setError] = useState(null);

    const handleGenerate = async (e) => {
        e.preventDefault();
        if (!inputMessage.trim()) return;

        setIsGenerating(true);
        setError(null);
        setResponseText('');

        try {
            const params = new URLSearchParams({ message: inputMessage });
            const response = await fetch(`/ai/generate?${params}`, {
                method: 'GET'
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const json = await response.json();

            // The sample controller returns Map.of("generation", text)
            // We safely extract the string
            const generatedText = json['generation'];

            if (generatedText) {
                setResponseText(generatedText);
            } else if (json['error'] || json['message']) {
                setError(json['error'] || json['message']);
            } else {
                setError("Unknown response format");
            }

        } catch (error) {
            console.error("Error fetching data:", error);
            setError("Failed to generate response. Check console.");
        } finally {
            setIsGenerating(false);
        }
    };

    return (
        <div className="chatbot-container">

            {/* Input Section */}
            <form onSubmit={handleGenerate} className="chat-form">
                <input
                    type="text"
                    value={inputMessage}
                    onChange={(e) => setInputMessage(e.target.value)}
                    placeholder="Type a message here..."
                    disabled={isGenerating}
                    className="chat-input"
                />
                <button type="submit" disabled={isGenerating} className="chat-btn">
                    {isGenerating ? 'Generating...' : 'Generate'}
                </button>
            </form>

            {/* Display Section */}
            <div className="chat-display">
                {error && <div className="error-message">{error}</div>}

                {responseText && (
                    <div className="response-content">
                        <h3>Response:</h3>
                        <div className="response-text">
                            {/* Optional: Simple formatting for newlines */}
                            {responseText.split('\\n').map((line, index) => (
                                <div key={index}>{line}</div>
                            ))}
                        </div>
                    </div>
                )}

                {!responseText && !error && !isGenerating && (
                    <div className="placeholder-text">
                        Ask me a question or tell me a joke!
                    </div>
                )}

                {isGenerating && !responseText && (
                    <div className="loading-text">Thinking...</div>
                )}
            </div>
        </div>
    );
}

export default Chatbot;