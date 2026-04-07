import React, { useState, useEffect, useRef } from 'react';

function Chatbot() {
    const [inputMessage, setInputMessage] = useState('');
    const [responseText, setResponseText] = useState('');
    const [isGenerating, setIsGenerating] = useState(false);
    const [error, setError] = useState(null);

    // Store the active requestId and polling interval so we can cancel them
    const pollingInterval = useRef(null);
    const activeRequestId = useRef(null);

    // stop polling when component is unmounted
    useEffect(() => {
        return () => stopPolling();
    }, []);

    const stopPolling = () => {
        if (pollingInterval.current) {
            clearInterval(pollingInterval.current);
            pollingInterval.current = null;
        }
    };

    const pollForResponse = (requestId) => {
        pollingInterval.current = setInterval(async () => {
            if (activeRequestId.current !== requestId) {
                stopPolling();
                return;
            }

            try {
                const res = await fetch(`/ai/response/${requestId}`);
                if (!res.ok) throw new Error(`Poll failed: ${res.status}`);

                const json = await res.json();
                console.log("poll response:", json) //testing polling time out

                if (json.status === 'complete') {
                    stopPolling();
                    setResponseText(json.answer);
                    setIsGenerating(false);
                }

            } catch (err) {
                stopPolling();
                setError("Lost connection while waiting for response.");
                setIsGenerating(false);
                console.error("Polling error:", err);
            }
        }, 2000); // poll every 2 seconds
    };

    const handleGenerate = async (e) => {
        e.preventDefault();
        if (!inputMessage.trim()) return;

        // Cancel any previous in-flight request
        stopPolling();
        setIsGenerating(true);
        setError(null);
        setResponseText('');

        try {
            const params = new URLSearchParams({ message: inputMessage });
            const res = await fetch(`/ai/generate?${params}`, { method: 'GET' });

            if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);

            const json = await res.json();

            if (!json.requestId) {
                setError("Server did not return a requestId.");

                setIsGenerating(false);
                return;
            }

            // Store requestId and begin polling
            activeRequestId.current = json.requestId;
            pollForResponse(json.requestId);

        } catch (err) {
            console.error("Error sending message:", err);
            setError("Failed to send message. Check console.");
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
                    {isGenerating ? 'Waiting for response...' : 'Generate'}
                </button>
            </form>

            {/* Display Section */}
            <div className="chat-display">
                {error && <div className="error-message">{error}</div>}

                {responseText && (
                    <div className="response-content">
                        <h3>Response:</h3>
                        <div className="response-text">
                            {responseText.split('\n').map((line, index) => (
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
                    <div className="loading-text">
                        Processing via Kafka... this may take a moment.
                    </div>
                )}
            </div>
        </div>
    );
}

export default Chatbot;