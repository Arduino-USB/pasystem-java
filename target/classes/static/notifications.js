// Dynamic CSS Injection for top-right stacking notifications
(function() {
    const style = document.createElement('style');
    style.innerHTML = `
        /* The master container in the top-right corner */
        #custom-noti-container {
            position: fixed;
            top: 65px; /* Sits 5px right below your 60px topbar */
            right: 15px; /* Shifted to the right side */
            width: 320px;
            max-height: calc(100vh - 80px); /* Keeps it from flowing off screen */
            overflow-y: auto; /* Vertically scrolls if there are too many notifications */
            display: flex;
            flex-direction: column;
            gap: 12px; /* Spaces out notifications vertically */
            z-index: 100000;
            pointer-events: none; /* Allows clicking items under the empty container space */
            padding-right: 5px; /* Breathing room for scrollbar */
        }
        
        /* Individual Notification Box adjustments */
        .custom-noti-box {
            background: white;
            padding: 15px;
            border-radius: 8px;
            width: 100%;
            box-sizing: border-box;
            border: 1px solid #e2e8f0; /* Slight border so it stands out against light backgrounds */
            box-shadow: 0 4px 12px rgba(0,0,0,0.08); /* Softened shadow to blend cleanly with the border */
            text-align: left; 
            border-right: 5px solid #006bb3; /* Flipped accent line to the right edge */
            pointer-events: auto; /* Restores button clicks inside notifications */
            animation: slideInRight 0.2s ease-out;
        }
        
        .custom-noti-title {
            margin-top: 0;
            margin-bottom: 5px;
            color: #333;
            font-size: 16px;
        }
        
        .custom-noti-msg {
            color: #666;
            margin: 5px 0 12px 0;
            font-size: 13px;
            line-height: 1.4;
        }
        
        .custom-noti-btns {
            display: flex;
            gap: 8px;
            justify-content: flex-end; /* Align actions to the bottom right */
            flex-wrap: wrap;
        }
        
        .custom-noti-btn {
            padding: 6px 12px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 12px;
            background: #006bb3;
            color: white;
            transition: background 0.15s;
        }
        
        .custom-noti-btn:hover {
            background: #014573;
        }

        /* Slide-in transition from the right edge */
        @keyframes slideInRight {
            from { transform: translateX(50px); opacity: 0; }
            to { transform: translateX(0); opacity: 1; }
        }
    `;
    document.head.appendChild(style);
})();

/**
 * Custom Notification API Engine
 * @param {string} title 
 * @param {string} message 
 * @param {Object} buttons - Format: {"Button Name": function or string JS code, ...}
 * @param {boolean} dismisses - Clicking any button clears the box window
 */
function showNotification(title, message, buttons = {}, dismisses = true) {
    // 1. Check for or build the structural master container
    let container = document.getElementById('custom-noti-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'custom-noti-container';
        document.body.appendChild(container);
    }

    // 2. Build the target notification block
    const box = document.createElement('div');
    box.className = 'custom-noti-box';

    const h3 = document.createElement('h3');
    h3.className = 'custom-noti-title';
    h3.innerText = title;
    box.appendChild(h3);

    const p = document.createElement('p');
    p.className = 'custom-noti-msg';
    p.innerText = message;
    box.appendChild(p);

    const btnContainer = document.createElement('div');
    btnContainer.className = 'custom-noti-btns';

    // Build functional buttons
    for (const [btnName, handler] of Object.entries(buttons)) {
        const btn = document.createElement('button');
        btn.className = 'custom-noti-btn';
        btn.innerText = btnName;
        btn.onclick = function() {
            if (typeof handler === 'function') {
                handler();
            } else if (typeof handler === 'string') {
                new Function(handler)();
            }
            if (dismisses) {
                box.remove();
            }
        };
        btnContainer.appendChild(btn);
    }

    // Default Dismiss button if no functional configuration object is supplied
    if (Object.keys(buttons).length === 0) {
        const okBtn = document.createElement('button');
        okBtn.className = 'custom-noti-btn';
        okBtn.innerText = 'Dismiss';
        okBtn.onclick = function() { box.remove(); };
        btnContainer.appendChild(okBtn);
    }

    box.appendChild(btnContainer);
    
    // 3. Append to the layout stack
    container.appendChild(box);
    
    return box; // Returns the specific handle reference so you can still programmatically trigger .remove() elsewhere
}
