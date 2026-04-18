package com.example.btl_novelworld;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.btl_novelworld.models.Book;
import com.example.btl_novelworld.models.Chapter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReaderActivity extends AppCompatActivity {

    private TextView txtBookTitle, txtCurrentChapter, tvChapterContent, btnPrevChapter, btnNextChapter;
    private TextView txtAudioSpeed, txtAudioCurrentPart, btnToggleHighlight;
    private ImageView btnBack, btnTableOfContents;
    private FrameLayout btnScrollTop;
    private ScrollView scrollViewContent;

    private SeekBar audioSeekBar;
    private ImageView btnReplayAudio, btnPrevAudioPart, btnNextAudioPart, btnAudioSettings, imgPlayPauseAudio;
    private FrameLayout btnPlayPauseAudio;

    private FirebaseFirestore db;
    private String bookId;
    private String chapterId;
    private Chapter currentChapter;

    private TextToSpeech textToSpeech;
    private boolean isTtsReady = false;
    private boolean isAudioPlaying = false;

    private final List<String> audioParts = new ArrayList<>();
    private int currentAudioIndex = 0;
    private String fullChapterContent = "";

    private final float[] speechRates = new float[]{0.75f, 1.0f, 1.25f, 1.5f};
    private final String[] speechRateLabels = new String[]{"0.75x", "1.0x", "1.25x", "1.5x"};
    private int currentSpeechRateIndex = 1;

    private boolean isHighlightEnabled = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_detail);

        db = FirebaseFirestore.getInstance();
        bookId = getIntent().getStringExtra("bookId");
        chapterId = getIntent().getStringExtra("chapterId");

        if (bookId == null || chapterId == null) {
            finish();
            return;
        }

        initViews();
        initTextToSpeech();
        setupActions();
        loadBookInfo();
        loadCurrentChapter();
    }

    private void initViews() {
        txtBookTitle = findViewById(R.id.txt_book_title);
        txtCurrentChapter = findViewById(R.id.txt_current_chapter);
        tvChapterContent = findViewById(R.id.tvChapterContent);
        btnPrevChapter = findViewById(R.id.btnPrevChapter);
        btnNextChapter = findViewById(R.id.btnNextChapter);
        btnBack = findViewById(R.id.btnBack);
        btnTableOfContents = findViewById(R.id.btnTableOfContents);
        btnScrollTop = findViewById(R.id.btnScrollTop);
        scrollViewContent = findViewById(R.id.scrollViewContent);

        audioSeekBar = findViewById(R.id.audioSeekBar);
        txtAudioSpeed = findViewById(R.id.txtAudioSpeed);
        txtAudioCurrentPart = findViewById(R.id.txtAudioCurrentPart);
        btnReplayAudio = findViewById(R.id.btnReplayAudio);
        btnPrevAudioPart = findViewById(R.id.btnPrevAudioPart);
        btnNextAudioPart = findViewById(R.id.btnNextAudioPart);
        btnAudioSettings = findViewById(R.id.btnAudioSettings);
        imgPlayPauseAudio = findViewById(R.id.imgPlayPauseAudio);
        btnPlayPauseAudio = findViewById(R.id.btnPlayPauseAudio);
        btnToggleHighlight = findViewById(R.id.btnToggleHighlight);
        updateHighlightButtonText();

        updateAudioSpeedText();
        updatePlayPauseIcon();
        updateAudioCurrentPartText();
    }

    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(new Locale("vi", "VN"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    textToSpeech.setLanguage(Locale.US);
                }

                isTtsReady = true;
                textToSpeech.setSpeechRate(speechRates[currentSpeechRateIndex]);

                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        runOnUiThread(() -> {
                            audioSeekBar.setProgress(currentAudioIndex);

                            if (isAudioPlaying) {
                                if (currentAudioIndex < audioParts.size() - 1) {
                                    currentAudioIndex++;
                                    updateAudioCurrentPartText();
                                    highlightCurrentAudioPart();
                                    speakCurrentPart();
                                } else {
                                    stopAudioUiOnly();
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(String utteranceId) {
                        runOnUiThread(() -> stopAudioUiOnly());
                    }
                });
            } else {
                Toast.makeText(this, "Không khởi tạo được audio", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupActions() {
        btnBack.setOnClickListener(v -> finish());

        btnPrevChapter.setOnClickListener(v -> goToPreviousChapter());
        btnNextChapter.setOnClickListener(v -> goToNextChapter());

        btnTableOfContents.setOnClickListener(v -> openChapterList());
        btnScrollTop.setOnClickListener(v -> scrollToTop());

        btnReplayAudio.setOnClickListener(v -> replayAudioFromStart());
        btnPrevAudioPart.setOnClickListener(v -> goToPreviousChapter());
        btnNextAudioPart.setOnClickListener(v -> goToNextChapter());
        btnPlayPauseAudio.setOnClickListener(v -> toggleAudioPlayPause());
        btnAudioSettings.setOnClickListener(v -> showAudioSpeedDialog());
        btnToggleHighlight.setOnClickListener(v -> toggleHighlight());

        audioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (audioParts.isEmpty()) return;

                currentAudioIndex = seekBar.getProgress();
                updateAudioCurrentPartText();
                highlightCurrentAudioPart();

                if (isAudioPlaying) {
                    speakCurrentPart();
                }
            }
        });
    }

    private void toggleHighlight() {
        isHighlightEnabled = !isHighlightEnabled;
        updateHighlightButtonText();
        highlightCurrentAudioPart();
    }

    private void updateHighlightButtonText() {
        if (btnToggleHighlight == null) return;

        if (isHighlightEnabled) {
            btnToggleHighlight.setText("Tắt highlight");
        } else {
            btnToggleHighlight.setText("Bật highlight");
        }
    }

    private void openChapterList() {
        Intent intent = new Intent(this, ChapterListActivity.class);
        intent.putExtra("bookId", bookId);
        startActivity(intent);
    }

    private void scrollToTop() {
        if (scrollViewContent != null) {
            scrollViewContent.post(() -> scrollViewContent.smoothScrollTo(0, 0));
        }
    }

    private void loadBookInfo() {
        db.collection("Books")
                .document(bookId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Book book = snapshot.toObject(Book.class);
                    if (book != null) {
                        txtBookTitle.setText(book.getTitle());
                    }
                });
    }

    private void loadCurrentChapter() {
        db.collection("Books")
                .document(bookId)
                .collection("Chapters")
                .document(chapterId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    Chapter chapter = snapshot.toObject(Chapter.class);
                    if (chapter == null) return;

                    chapter.setChapterId(snapshot.getId());
                    currentChapter = chapter;

                    txtCurrentChapter.setText("Chương " + chapter.getChapterNumber());

                    fullChapterContent = chapter.getContent() == null ? "" : chapter.getContent();
                    tvChapterContent.setText(fullChapterContent);

                    saveReadingHistory(chapter.getChapterId());
                    prepareAudioContent(fullChapterContent);
                    scrollToTop();
                });
    }

    private void goToPreviousChapter() {
        if (currentChapter == null) return;

        long currentNumber = currentChapter.getChapterNumber();

        db.collection("Books")
                .document(bookId)
                .collection("Chapters")
                .orderBy("chapterNumber", Query.Direction.DESCENDING)
                .startAfter(currentNumber)
                .limit(1)
                .get()
                .addOnSuccessListener(result -> {
                    if (result.isEmpty()) {
                        Toast.makeText(this, "Đây là chương đầu", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    stopAudioFully();
                    DocumentSnapshot doc = result.getDocuments().get(0);
                    chapterId = doc.getId();
                    loadCurrentChapter();
                });
    }

    private void goToNextChapter() {
        if (currentChapter == null) return;

        long currentNumber = currentChapter.getChapterNumber();

        db.collection("Books")
                .document(bookId)
                .collection("Chapters")
                .orderBy("chapterNumber", Query.Direction.ASCENDING)
                .startAfter(currentNumber)
                .limit(1)
                .get()
                .addOnSuccessListener(result -> {
                    if (result.isEmpty()) {
                        Toast.makeText(this, "Đây là chương cuối", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    stopAudioFully();
                    DocumentSnapshot doc = result.getDocuments().get(0);
                    chapterId = doc.getId();
                    loadCurrentChapter();
                });
    }

    private void prepareAudioContent(String content) {
        audioParts.clear();
        currentAudioIndex = 0;
        stopAudioUiOnly();

        if (content == null) content = "";
        content = content.trim();

        if (content.isEmpty()) {
            audioSeekBar.setMax(0);
            audioSeekBar.setProgress(0);
            updatePlayPauseIcon();
            updateAudioCurrentPartText();
            tvChapterContent.setText("");
            return;
        }

        audioParts.addAll(splitIntoParagraphChunks(content, 300));

        audioSeekBar.setMax(Math.max(audioParts.size() - 1, 0));
        audioSeekBar.setProgress(0);
        updatePlayPauseIcon();
        updateAudioCurrentPartText();
        highlightCurrentAudioPart();
    }

    private void toggleAudioPlayPause() {
        if (!isTtsReady) {
            Toast.makeText(this, "Text to Speech chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            return;
        }

        if (audioParts.isEmpty()) {
            Toast.makeText(this, "Chưa có nội dung để đọc", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isAudioPlaying) {
            pauseAudio();
        } else {
            startAudio();
        }
    }

    private void startAudio() {
        isAudioPlaying = true;
        updatePlayPauseIcon();
        updateAudioCurrentPartText();
        highlightCurrentAudioPart();
        speakCurrentPart();
    }

    private void pauseAudio() {
        isAudioPlaying = false;
        updatePlayPauseIcon();
        updateAudioCurrentPartText();

        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    private void stopAudioUiOnly() {
        isAudioPlaying = false;
        updatePlayPauseIcon();
        updateAudioCurrentPartText();
        highlightCurrentAudioPart();

        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    private void stopAudioFully() {
        isAudioPlaying = false;
        currentAudioIndex = 0;
        audioSeekBar.setProgress(0);
        updatePlayPauseIcon();
        updateAudioCurrentPartText();
        highlightCurrentAudioPart();

        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    private void replayAudioFromStart() {
        if (audioParts.isEmpty()) return;

        currentAudioIndex = 0;
        audioSeekBar.setProgress(0);
        updateAudioCurrentPartText();
        highlightCurrentAudioPart();
        startAudio();
    }

    private void speakCurrentPart() {
        if (!isTtsReady || audioParts.isEmpty()) return;
        if (currentAudioIndex < 0 || currentAudioIndex >= audioParts.size()) return;

        updateAudioCurrentPartText();
        highlightCurrentAudioPart();

        String text = audioParts.get(currentAudioIndex);
        String utteranceId = "chapter_part_" + currentAudioIndex;
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }

    private void showAudioSpeedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn tốc độ đọc");

        builder.setSingleChoiceItems(speechRateLabels, currentSpeechRateIndex, (dialog, which) -> {
            currentSpeechRateIndex = which;

            if (textToSpeech != null) {
                textToSpeech.setSpeechRate(speechRates[currentSpeechRateIndex]);
            }

            updateAudioSpeedText();
            dialog.dismiss();
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void updateAudioSpeedText() {
        if (txtAudioSpeed != null) {
            txtAudioSpeed.setText("Tốc độ: " + speechRateLabels[currentSpeechRateIndex]);
        }
    }

    private void updatePlayPauseIcon() {
        if (imgPlayPauseAudio == null) return;

        if (isAudioPlaying) {
            imgPlayPauseAudio.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            imgPlayPauseAudio.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void updateAudioCurrentPartText() {
        if (txtAudioCurrentPart == null) return;

        if (audioParts.isEmpty()) {
            txtAudioCurrentPart.setText("Chưa có nội dung audio");
            return;
        }

        if (isAudioPlaying) {
            txtAudioCurrentPart.setText("Đang đọc: đoạn " + (currentAudioIndex + 1) + "/" + audioParts.size());
        } else {
            txtAudioCurrentPart.setText("Đang chọn: đoạn " + (currentAudioIndex + 1) + "/" + audioParts.size());
        }
    }

    private void highlightCurrentAudioPart() {
        if (fullChapterContent == null || fullChapterContent.isEmpty()) {
            tvChapterContent.setText("");
            return;
        }

        if (!isHighlightEnabled) {
            tvChapterContent.setText(fullChapterContent);
            return;
        }

        if (audioParts.isEmpty() || currentAudioIndex < 0 || currentAudioIndex >= audioParts.size()) {
            tvChapterContent.setText(fullChapterContent);
            return;
        }

        String currentPart = audioParts.get(currentAudioIndex);
        int start = fullChapterContent.indexOf(currentPart);

        if (start < 0) {
            tvChapterContent.setText(fullChapterContent);
            return;
        }

        int end = start + currentPart.length();

        SpannableString spannable = new SpannableString(fullChapterContent);
        spannable.setSpan(new ForegroundColorSpan(0xFF1976D2), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvChapterContent.setText(spannable);

        scrollViewContent.post(() -> {
            if (tvChapterContent.getLayout() != null) {
                int line = tvChapterContent.getLayout().getLineForOffset(start);
                int y = tvChapterContent.getLayout().getLineTop(line);
                scrollViewContent.smoothScrollTo(0, Math.max(y - 120, 0));
            }
        });
    }

    private void saveReadingHistory(String lastReadChapterId) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        HistoryLibraryPayload payload = new HistoryLibraryPayload(bookId, "history", lastReadChapterId);

        db.collection("Users")
                .document(uid)
                .collection("Library")
                .whereEqualTo("bookId", bookId)
                .whereEqualTo("type", "history")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        queryDocumentSnapshots.getDocuments().get(0).getReference().set(payload);
                    } else {
                        db.collection("Users")
                                .document(uid)
                                .collection("Library")
                                .add(payload);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    private List<String> splitIntoParagraphChunks(String text, int maxLength) {
        List<String> result = new ArrayList<>();

        String[] paragraphs = text.split("\\n\\n+");
        for (String paragraph : paragraphs) {
            String cleaned = paragraph.trim();
            if (cleaned.isEmpty()) continue;

            if (cleaned.length() <= maxLength) {
                result.add(cleaned);
            } else {
                result.addAll(splitLongTextBySentence(cleaned, maxLength));
            }
        }

        return result;
    }

    private List<String> splitLongTextBySentence(String text, int maxLength) {
        List<String> result = new ArrayList<>();
        String[] sentences = text.split("(?<=[.!?…])\\s+");
        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) continue;

            if (trimmed.length() > maxLength) {
                if (currentChunk.length() > 0) {
                    result.add(currentChunk.toString().trim());
                    currentChunk.setLength(0);
                }
                result.addAll(splitVeryLongSentence(trimmed, maxLength));
                continue;
            }

            if (currentChunk.length() == 0) {
                currentChunk.append(trimmed);
            } else if (currentChunk.length() + 1 + trimmed.length() <= maxLength) {
                currentChunk.append(" ").append(trimmed);
            } else {
                result.add(currentChunk.toString().trim());
                currentChunk.setLength(0);
                currentChunk.append(trimmed);
            }
        }

        if (currentChunk.length() > 0) {
            result.add(currentChunk.toString().trim());
        }

        return result;
    }

    private List<String> splitVeryLongSentence(String text, int maxLength) {
        List<String> result = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + maxLength, text.length());

            if (end < text.length()) {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > start) {
                    end = lastSpace;
                }
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                result.add(chunk);
            }

            start = end;
        }

        return result;
    }

    public static class HistoryLibraryPayload {
        public String bookId;
        public String type;
        public String lastReadChapterId;
        public Timestamp timestamp;

        public HistoryLibraryPayload() {
        }

        public HistoryLibraryPayload(String bookId, String type, String lastReadChapterId) {
            this.bookId = bookId;
            this.type = type;
            this.lastReadChapterId = lastReadChapterId;
            this.timestamp = Timestamp.now();
        }
    }
}