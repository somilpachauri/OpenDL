chrome.downloads.onCreated.addListener((downloadItem) => {
    // FIX 1: Capital 'W' in startsWith
    if (downloadItem.state === "interrupted" || downloadItem.url.startsWith("blob:")) {
        return;
    }

    fetch('http://127.0.0.1:12345/catch', {
        method: 'POST', 
        body: downloadItem.url,
        headers: {
            'Content-Type': 'text/plain'
        }
    }) // FIX 2: Added the missing ')' right here before the .then
    .then(response => {
        if (response.ok) {
            console.log("Successfully sent URL to OpenDL.");
            chrome.downloads.cancel(downloadItem.id);
            chrome.downloads.erase({ id: downloadItem.id }); 
        } else {
            console.error("OpenDL server responded with an error.");
        }
    })
    .catch(err => {
        console.warn("OpenDL is not running. Falling back to Chrome's default downloader.", err);
    });
});