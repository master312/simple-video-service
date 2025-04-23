document.addEventListener('DOMContentLoaded', function() {
    const videoList = document.getElementById('videoList');

    async function fetchAndDisplayVideos() {
        try {
            const response = await fetch('/uploads');
            const videos = await response.json();
            displayVideos(videos);
        } catch (error) {
            console.error('Error fetching videos:', error);
            alert('Error loading video list: ' + error.message);
        }
    }

    function displayVideos(videos) {
        videoList.innerHTML = '';
        
        videos.forEach(video => {
            const row = document.createElement('tr');

            const nameCell = document.createElement('td');
            nameCell.textContent = video.originalFilename;
            row.appendChild(nameCell);
            
            // Actions cell
            const actionsCell = document.createElement('td');
            const statusSpan = document.createElement('span');
            statusSpan.textContent = `Status: ${video.status}`;
            statusSpan.style.marginRight = '10px';
            actionsCell.appendChild(statusSpan);
            
            // Downoad button if video is available
            if (video.status === 'AVAILABLE') {
                const downloadButton = document.createElement('button');
                downloadButton.textContent = 'Download';
                downloadButton.onclick = () => window.location.href = `/download/${video.id}`;
                actionsCell.appendChild(downloadButton);
            }
            
            row.appendChild(actionsCell);
            videoList.appendChild(row);
        });
    }

    // Init load of videos
    fetchAndDisplayVideos();

    // Upload button and upload stuff....
    const uploadForm = document.getElementById('uploadForm');
    const uploadIndicator = document.getElementById('uploadIndicator');
    uploadForm.addEventListener('submit', async function(e) {
        e.preventDefault();
        
        const formData = new FormData();
        const fileInput = uploadForm.querySelector('input[type="file"]');
        
        if (fileInput.files.length === 0) {
            alert('Please select a file to upload');
            return;
        }

        formData.append('file', fileInput.files[0]);
        uploadIndicator.style.display = 'block';

        try {
            const response = await fetch('/upload', {
                method: 'POST',
                body: formData
            });

            const result = await response.text();
            
            if (response.ok) {
                fileInput.value = '';
                await fetchAndDisplayVideos();
            }

            alert(result);
        } catch (error) {
            alert('Error during upload: ' + error.message);
        } finally {
            uploadIndicator.style.display = 'none';
        }
    });
});
