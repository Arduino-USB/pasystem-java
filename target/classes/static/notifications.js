// Dynamic CSS Injection for notifications
(function() {
    const style = document.createElement('style');
    style.innerHTML = `
        .custom-noti-overlay {
            position: fixed;
            inset: 0;
            background: rgba(0,0,0,0.6);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 100000;
            font-family: sans-serif;
        }
        .custom-noti-box {
            background: white;
            padding: 25px;
            border-radius: 10px;
            min-width: 300px;
            max-width: 450px;
            box-shadow: 0 4px 15px rgba(0,0,0,0.3);
            text-align: center;
        }
        .custom-noti-title {
            margin-top: 0;
            color: #333;
            font-size: 20px;
        }
        .custom-noti-msg {
            color: #666;
            margin: 15px 0 20px 0;
            font-size: 15px;
            line-height: 1.4;
        }
        .custom-noti-btns {
            display: flex;
            gap: 10px;
            justify-content: center;
            flex-wrap: wrap;
        }
        .custom-noti-btn {
            padding: 8px 16px;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            font-size: 14px;
            background: #006bb3;
            color: white;
            transition: background 0.2s;
        }
        .custom-noti-btn:hover {
            background: #014573;
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
    const overlay = document.createElement('div');
    overlay.className = 'custom-noti-overlay';

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
                overlay.remove();
            }
        };
        btnContainer.appendChild(btn);
    }

    // Default Okay button block if empty fallback is needed
    if (Object.keys(buttons).length === 0) {
        const okBtn = document.createElement('button');
        okBtn.className = 'custom-noti-btn';
        okBtn.innerText = 'Okay';
        okBtn.onclick = function() { overlay.remove(); };
        btnContainer.appendChild(okBtn);
    }

    box.appendChild(btnContainer);
    overlay.appendChild(box);
    document.body.appendChild(overlay);
    
    return overlay; // Returns handle reference if manual dismissal is needed outside
}