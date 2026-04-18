package com.example.btl_novelworld;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.btl_novelworld.database.AppDatabase;
import com.example.btl_novelworld.models.Book;
import com.example.btl_novelworld.models.Chapter;
import com.example.btl_novelworld.models.LocalBook;
import com.example.btl_novelworld.models.LocalChapter;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReaderActivity extends AppCompatActivity {
    private LinearLayout navHome, navExplore, navLibrary, navProfile;
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

    // --- CÁC BIẾN PHỤC VỤ OFFLINE ---
    private boolean isOffline = false;
    private AppDatabase appDb;
    private ExecutorService executorService;

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

    private static final String PREF_AUDIO_STATE = "reader_audio_state";
    private static final String KEY_BOOK_ID = "book_id";
    private static final String KEY_CHAPTER_ID = "chapter_id";
    private static final String KEY_AUDIO_INDEX = "audio_index";
    private static final String KEY_IS_AUDIO_PLAYING = "is_audio_playing";

    private final Handler sleepTimerHandler = new Handler(Looper.getMainLooper());
    private Runnable sleepTimerRunnable;
    private boolean stopWhenChapterEnds = false;

    private final String[] sleepTimerLabels = {
            "Tắt",
            "5 phút",
            "10 phút",
            "15 phút",
            "20 phút",
            "30 phút",
            "45 phút",
            "1 giờ",
            "Khi hết chương này"
    };
    private int selectedSleepTimerIndex = 0;

    private boolean shouldAutoPlayAfterChapterLoad = false;
    private boolean shouldAskResumeAudio = false;
    private int pendingResumeAudioIndex = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_detail);

        db = FirebaseFirestore.getInstance();
        bookId = getIntent().getStringExtra("bookId");
        chapterId = getIntent().getStringExtra("chapterId");

        // NHẬN CỜ BÁO HIỆU OFFLINE TỪ INTENT
        isOffline = getIntent().getBooleanExtra("isOffline", false);

        // Khởi tạo Database nội bộ
        appDb = AppDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();

        if (bookId == null || chapterId == null) {
            Toast.makeText(this, "Lỗi dữ liệu đầu vào", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initTextToSpeech();
        setupActions();
        loadBookInfo();
        restoreSavedAudioStateIfAny();
        loadCurrentChapter();
        setupNavigation();
        setupTextJustification();
    }
    private void setupTextJustification() {
        TextView tvContent = findViewById(R.id.tvChapterContent);

        // Kiểm tra nếu phiên bản Android từ 8.0 (Oreo) trở lên
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            tvContent.setJustificationMode(android.graphics.text.LineBreaker.JUSTIFICATION_MODE_INTER_WORD);
        }
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
        navHome = findViewById(R.id.navHome);
        navExplore = findViewById(R.id.navExplore);
        navLibrary = findViewById(R.id.navLibrary);
        navProfile = findViewById(R.id.navProfile);

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

                            if (!isAudioPlaying) return;

                            if (currentAudioIndex < audioParts.size() - 1) {
                                currentAudioIndex++;
                                pendingResumeAudioIndex = currentAudioIndex;
                                updateAudioCurrentPartText();
                                highlightCurrentAudioPart();
                                saveAudioReadingState();
                                speakCurrentPart();
                            } else {
                                if (stopWhenChapterEnds) {
                                    isAudioPlaying = false;
                                    stopWhenChapterEnds = false;
                                    selectedSleepTimerIndex = 0;
                                    updatePlayPauseIcon();
                                    updateAudioCurrentPartText();
                                    saveAudioReadingState();
                                    Toast.makeText(ReaderActivity.this, "Đã dừng khi hết chương này", Toast.LENGTH_SHORT).show();
                                } else {
                                    autoMoveToNextChapterAfterAudioFinished();
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(String utteranceId) {
                        runOnUiThread(() -> {
                            isAudioPlaying = false;
                            updatePlayPauseIcon();
                            saveAudioReadingState();
                            Toast.makeText(ReaderActivity.this, "Đọc audio bị lỗi", Toast.LENGTH_SHORT).show();
                        });
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
        btnPrevAudioPart.setOnClickListener(v -> goToPreviousAudioPart());
        btnNextAudioPart.setOnClickListener(v -> goToNextAudioPart());
        btnPlayPauseAudio.setOnClickListener(v -> toggleAudioPlayPause());
        btnAudioSettings.setOnClickListener(v -> showAudioSettingsBottomSheet());
        btnToggleHighlight.setOnClickListener(v -> toggleHighlight());

        audioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (audioParts.isEmpty()) return;

                currentAudioIndex = seekBar.getProgress();
                pendingResumeAudioIndex = currentAudioIndex;
                updateAudioCurrentPartText();
                highlightCurrentAudioPart();
                saveAudioReadingState();

                if (isAudioPlaying) {
                    speakCurrentPart();
                }
            }
        });
    }

    private void setupNavigation() {
        navHome.setOnClickListener(v -> navigateTo(HomeActivity.class));
        navExplore.setOnClickListener(v -> navigateTo(ExploreActivity.class));
        navLibrary.setOnClickListener(v -> navigateTo(LibraryActivity.class));
        navProfile.setOnClickListener(v -> navigateTo(ProfileActivity.class));
    }

    private void navigateTo(Class<?> targetActivity) {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
        isAudioPlaying = false;
        Intent intent = new Intent(ReaderActivity.this, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    // ==========================================
    // CÁC HÀM TẢI DỮ LIỆU ĐÃ ĐƯỢC RẼ NHÁNH OFFLINE
    // ==========================================

    private void loadBookInfo() {
        if (isOffline) {
            executorService.execute(() -> {
                LocalBook book = appDb.offlineDao().getBookById(bookId);
                if (book != null) {
                    runOnUiThread(() -> txtBookTitle.setText(book.title));
                }
            });
        } else {
            db.collection("Books").document(bookId).get().addOnSuccessListener(snapshot -> {
                Book book = snapshot.toObject(Book.class);
                if (book != null) {
                    txtBookTitle.setText(book.getTitle());
                }
            });
        }
    }

    private void incrementViewCountInReader() {
        FirebaseFirestore.getInstance().collection("Books").document(bookId)
                .update(
                        "viewsCount", FieldValue.increment(1),
                        "viewsWeek", FieldValue.increment(1),
                        "viewsMonth", FieldValue.increment(1)
                );
    }

    private void loadCurrentChapter() {
        if (isOffline) {
            // TẢI CHƯƠNG OFFLINE TỪ ROOM DATABASE
            executorService.execute(() -> {
                LocalChapter localChapter = appDb.offlineDao().getChapterById(chapterId);

                if (localChapter == null) {
                    runOnUiThread(() -> Toast.makeText(ReaderActivity.this, "Không tìm thấy nội dung offline", Toast.LENGTH_SHORT).show());
                    return;
                }

                runOnUiThread(() -> {
                    // Ánh xạ LocalChapter sang Chapter để tương thích với code cũ của bạn
                    currentChapter = new Chapter();
                    currentChapter.setChapterId(localChapter.chapterId);
                    currentChapter.setChapterNumber(localChapter.chapterNumber);
                    currentChapter.setChapterTitle(localChapter.chapterTitle);
                    currentChapter.setContent(localChapter.content);

                    txtCurrentChapter.setText("Chương " + currentChapter.getChapterNumber());

                    fullChapterContent = currentChapter.getContent() == null ? "" : currentChapter.getContent();
                    tvChapterContent.setText(fullChapterContent);

                    // Offline thì không cần lưu lịch sử lên Firebase để tránh lỗi mạng
                    prepareAudioContent(fullChapterContent);
                    scrollToTop();

                    if (shouldAskResumeAudio) {
                        askResumeAudioIfNeeded();
                    } else if (shouldAutoPlayAfterChapterLoad) {
                        shouldAutoPlayAfterChapterLoad = false;
                        startAudio();
                    }
                });
            });

        } else {
            // TẢI CHƯƠNG ONLINE TỪ FIREBASE
            incrementViewCountInReader();
            db.collection("Books").document(bookId).collection("Chapters").document(chapterId)
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

                        if (shouldAskResumeAudio) {
                            askResumeAudioIfNeeded();
                        } else if (shouldAutoPlayAfterChapterLoad) {
                            shouldAutoPlayAfterChapterLoad = false;
                            startAudio();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Không tải được chương", Toast.LENGTH_SHORT).show());
        }
    }

    private void goToPreviousChapter() {
        if (currentChapter == null) return;
        long currentNumber = currentChapter.getChapterNumber();

        if (isOffline) {
            executorService.execute(() -> {
                // Truy vấn chương có số nhỏ hơn chương hiện tại
                LocalChapter prevChap = appDb.offlineDao().getPreviousChapter(bookId, currentNumber);

                runOnUiThread(() -> {
                    if (prevChap == null) {
                        Toast.makeText(this, "Đây là chương đầu", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    stopAudioFully();
                    chapterId = prevChap.chapterId;
                    resetAudioStateForNewChapter();
                    loadCurrentChapter();
                });
            });
        } else {
            db.collection("Books").document(bookId).collection("Chapters")
                    .orderBy("chapterNumber", Query.Direction.DESCENDING)
                    .startAfter(currentNumber).limit(1).get()
                    .addOnSuccessListener(result -> {
                        if (result.isEmpty()) {
                            Toast.makeText(this, "Đây là chương đầu", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        stopAudioFully();
                        chapterId = result.getDocuments().get(0).getId();
                        resetAudioStateForNewChapter();
                        loadCurrentChapter();
                    });
        }
    }

    private void goToNextChapter() {
        if (currentChapter == null) return;
        long currentNumber = currentChapter.getChapterNumber();

        if (isOffline) {
            executorService.execute(() -> {
                // Truy vấn chương có số lớn hơn chương hiện tại
                LocalChapter nextChap = appDb.offlineDao().getNextChapter(bookId, currentNumber);

                runOnUiThread(() -> {
                    if (nextChap == null) {
                        Toast.makeText(this, "Đây là chương cuối", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    stopAudioFully();
                    chapterId = nextChap.chapterId;
                    resetAudioStateForNewChapter();
                    loadCurrentChapter();
                });
            });
        } else {
            db.collection("Books").document(bookId).collection("Chapters")
                    .orderBy("chapterNumber", Query.Direction.ASCENDING)
                    .startAfter(currentNumber).limit(1).get()
                    .addOnSuccessListener(result -> {
                        if (result.isEmpty()) {
                            Toast.makeText(this, "Đây là chương cuối", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        stopAudioFully();
                        chapterId = result.getDocuments().get(0).getId();
                        resetAudioStateForNewChapter();
                        loadCurrentChapter();
                    });
        }
    }

    private void autoMoveToNextChapterAfterAudioFinished() {
        if (currentChapter == null) {
            stopAudioUiOnly();
            return;
        }

        long currentNumber = currentChapter.getChapterNumber();

        if (isOffline) {
            executorService.execute(() -> {
                LocalChapter nextChap = appDb.offlineDao().getNextChapter(bookId, currentNumber);
                runOnUiThread(() -> {
                    if (nextChap == null) {
                        isAudioPlaying = false;
                        updatePlayPauseIcon();
                        updateAudioCurrentPartText();
                        clearSavedAudioReadingState();
                        Toast.makeText(this, "Đã đọc hết chương cuối", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    chapterId = nextChap.chapterId;
                    resetAudioStateForAutoPlay();
                    loadCurrentChapter();
                });
            });
        } else {
            db.collection("Books").document(bookId).collection("Chapters")
                    .orderBy("chapterNumber", Query.Direction.ASCENDING)
                    .startAfter(currentNumber).limit(1).get()
                    .addOnSuccessListener(result -> {
                        if (result.isEmpty()) {
                            isAudioPlaying = false;
                            updatePlayPauseIcon();
                            updateAudioCurrentPartText();
                            clearSavedAudioReadingState();
                            Toast.makeText(this, "Đã đọc hết chương cuối", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        chapterId = result.getDocuments().get(0).getId();
                        resetAudioStateForAutoPlay();
                        loadCurrentChapter();
                    })
                    .addOnFailureListener(e -> stopAudioUiOnly());
        }
    }

    private void resetAudioStateForNewChapter() {
        currentAudioIndex = 0;
        pendingResumeAudioIndex = 0;
        shouldAskResumeAudio = false;
        shouldAutoPlayAfterChapterLoad = false;
        saveAudioReadingState();
    }

    private void resetAudioStateForAutoPlay() {
        currentAudioIndex = 0;
        pendingResumeAudioIndex = 0;
        shouldAskResumeAudio = false;
        shouldAutoPlayAfterChapterLoad = true;
        saveAudioReadingState();
    }

    // ==========================================
    // CÁC HÀM CÒN LẠI GIỮ NGUYÊN HOÀN TOÀN
    // ==========================================

    private void showAudioSettingsBottomSheet() {
        String[] options = {"Tốc độ đọc: " + speechRateLabels[currentSpeechRateIndex], "Hẹn giờ"};
        new AlertDialog.Builder(this)
                .setTitle("Cài đặt audio")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) showAudioSpeedDialog();
                    else if (which == 1) showSleepTimerBottomSheet();
                }).show();
    }

    private void restoreSavedAudioStateIfAny() {
        SharedPreferences prefs = getSharedPreferences(PREF_AUDIO_STATE, MODE_PRIVATE);
        String savedBookId = prefs.getString(KEY_BOOK_ID, null);
        String savedChapterId = prefs.getString(KEY_CHAPTER_ID, null);
        int savedAudioIndex = prefs.getInt(KEY_AUDIO_INDEX, 0);
        boolean savedWasPlaying = prefs.getBoolean(KEY_IS_AUDIO_PLAYING, false);

        if (savedBookId == null || savedChapterId == null) return;
        if (savedBookId.equals(bookId)) {
            chapterId = savedChapterId;
            pendingResumeAudioIndex = Math.max(savedAudioIndex, 0);
            shouldAskResumeAudio = true;
            shouldAutoPlayAfterChapterLoad = savedWasPlaying;
        }
    }

    private void saveAudioReadingState() {
        getSharedPreferences(PREF_AUDIO_STATE, MODE_PRIVATE).edit()
                .putString(KEY_BOOK_ID, bookId)
                .putString(KEY_CHAPTER_ID, chapterId)
                .putInt(KEY_AUDIO_INDEX, currentAudioIndex)
                .putBoolean(KEY_IS_AUDIO_PLAYING, isAudioPlaying)
                .apply();
    }

    private void clearSavedAudioReadingState() {
        getSharedPreferences(PREF_AUDIO_STATE, MODE_PRIVATE).edit().clear().apply();
    }

    private void askResumeAudioIfNeeded() {
        if (!shouldAskResumeAudio || audioParts.isEmpty()) return;
        shouldAskResumeAudio = false;

        if (pendingResumeAudioIndex < 0) pendingResumeAudioIndex = 0;
        if (pendingResumeAudioIndex >= audioParts.size()) pendingResumeAudioIndex = audioParts.size() - 1;

        final int resumeIndex = pendingResumeAudioIndex;

        new AlertDialog.Builder(this)
                .setTitle("Nghe tiếp")
                .setMessage("Bạn đang nghe dở ở đoạn " + (resumeIndex + 1) + "/" + audioParts.size() + ". Tiếp tục từ đây?")
                .setPositiveButton("Tiếp tục", (dialog, which) -> {
                    currentAudioIndex = resumeIndex;
                    audioSeekBar.setProgress(currentAudioIndex);
                    updateAudioCurrentPartText();
                    highlightCurrentAudioPart();
                    saveAudioReadingState();
                    if (shouldAutoPlayAfterChapterLoad) startAudio();
                })
                .setNegativeButton("Đọc từ đầu", (dialog, which) -> {
                    currentAudioIndex = 0;
                    pendingResumeAudioIndex = 0;
                    audioSeekBar.setProgress(0);
                    updateAudioCurrentPartText();
                    highlightCurrentAudioPart();
                    saveAudioReadingState();
                })
                .setCancelable(false)
                .show();
    }

    private void toggleHighlight() {
        isHighlightEnabled = !isHighlightEnabled;
        updateHighlightButtonText();
        highlightCurrentAudioPart();
    }

    private void updateHighlightButtonText() {
        if (btnToggleHighlight != null) {
            btnToggleHighlight.setText(isHighlightEnabled ? "Tắt highlight" : "Bật highlight");
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

        if (pendingResumeAudioIndex >= 0 && pendingResumeAudioIndex < audioParts.size()) {
            currentAudioIndex = pendingResumeAudioIndex;
        } else {
            currentAudioIndex = 0;
        }

        audioSeekBar.setProgress(currentAudioIndex);
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
        if (isAudioPlaying) pauseAudio();
        else startAudio();
    }

    private void startAudio() {
        isAudioPlaying = true;
        updatePlayPauseIcon();
        updateAudioCurrentPartText();
        highlightCurrentAudioPart();
        saveAudioReadingState();
        speakCurrentPart();
    }

    private void pauseAudio() {
        isAudioPlaying = false;
        updatePlayPauseIcon();
        updateAudioCurrentPartText();
        saveAudioReadingState();
        if (textToSpeech != null) textToSpeech.stop();
    }

    private void stopAudioUiOnly() {
        isAudioPlaying = false;
        updatePlayPauseIcon();
        updateAudioCurrentPartText();
        highlightCurrentAudioPart();
        saveAudioReadingState();
        if (textToSpeech != null) textToSpeech.stop();
    }

    private void stopAudioFully() {
        isAudioPlaying = false;
        currentAudioIndex = 0;
        pendingResumeAudioIndex = 0;
        audioSeekBar.setProgress(0);
        updatePlayPauseIcon();
        updateAudioCurrentPartText();
        highlightCurrentAudioPart();
        saveAudioReadingState();
        if (textToSpeech != null) textToSpeech.stop();
    }

    private void replayAudioFromStart() {
        if (audioParts.isEmpty()) return;
        currentAudioIndex = 0;
        pendingResumeAudioIndex = 0;
        audioSeekBar.setProgress(0);
        updateAudioCurrentPartText();
        highlightCurrentAudioPart();
        saveAudioReadingState();
        startAudio();
    }

    private void goToPreviousAudioPart() {
        if (audioParts.isEmpty()) return;
        if (currentAudioIndex > 0) {
            currentAudioIndex--;
            pendingResumeAudioIndex = currentAudioIndex;
            audioSeekBar.setProgress(currentAudioIndex);
            updateAudioCurrentPartText();
            highlightCurrentAudioPart();
            saveAudioReadingState();
            if (isAudioPlaying) speakCurrentPart();
        } else {
            Toast.makeText(this, "Đây là đoạn đầu", Toast.LENGTH_SHORT).show();
        }
    }

    private void goToNextAudioPart() {
        if (audioParts.isEmpty()) return;
        if (currentAudioIndex < audioParts.size() - 1) {
            currentAudioIndex++;
            pendingResumeAudioIndex = currentAudioIndex;
            audioSeekBar.setProgress(currentAudioIndex);
            updateAudioCurrentPartText();
            highlightCurrentAudioPart();
            saveAudioReadingState();
            if (isAudioPlaying) speakCurrentPart();
        } else {
            Toast.makeText(this, "Đây là đoạn cuối", Toast.LENGTH_SHORT).show();
        }
    }

    private void speakCurrentPart() {
        if (!isTtsReady || audioParts.isEmpty() || currentAudioIndex < 0 || currentAudioIndex >= audioParts.size()) return;

        updateAudioCurrentPartText();
        highlightCurrentAudioPart();

        String text = audioParts.get(currentAudioIndex);
        String utteranceId = "chapter_part_" + currentAudioIndex;
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }

    private void showSleepTimerBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        ScrollView scrollView = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, 24, 0, 24);

        View handle = new View(this);
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(120, 10);
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = 24;
        handle.setLayoutParams(handleParams);
        handle.setBackgroundColor(0xFFD0D0D0);
        container.addView(handle);

        for (int i = 0; i < sleepTimerLabels.length; i++) {
            final int index = i;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(32, 28, 32, 28);

            TextView checkView = new TextView(this);
            checkView.setTextSize(22);
            checkView.setTextColor(0xFF000000);
            checkView.setWidth(80);
            checkView.setText(selectedSleepTimerIndex == index ? "✓" : "");

            TextView labelView = new TextView(this);
            labelView.setText(sleepTimerLabels[index]);
            labelView.setTextSize(18);
            labelView.setTextColor(0xFF000000);

            row.addView(checkView);
            row.addView(labelView);

            row.setOnClickListener(v -> {
                selectedSleepTimerIndex = index;
                applySleepTimerSelection(index);
                dialog.dismiss();
            });
            container.addView(row);
        }

        scrollView.addView(container);
        dialog.setContentView(scrollView);
        dialog.show();
    }

    private void applySleepTimerSelection(int index) {
        cancelSleepTimer(true);
        stopWhenChapterEnds = false;

        switch (index) {
            case 0: Toast.makeText(this, "Đã tắt hẹn giờ", Toast.LENGTH_SHORT).show(); break;
            case 1: startSleepTimerMinutes(5); break;
            case 2: startSleepTimerMinutes(10); break;
            case 3: startSleepTimerMinutes(15); break;
            case 4: startSleepTimerMinutes(20); break;
            case 5: startSleepTimerMinutes(30); break;
            case 6: startSleepTimerMinutes(45); break;
            case 7: startSleepTimerMinutes(60); break;
            case 8:
                stopWhenChapterEnds = true;
                Toast.makeText(this, "Sẽ dừng khi hết chương này", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void startSleepTimerMinutes(int minutes) {
        if (minutes <= 0) return;
        if (minutes > 60) minutes = 60;
        cancelSleepTimer(true);

        sleepTimerRunnable = () -> {
            pauseAudio();
            selectedSleepTimerIndex = 0;
            Toast.makeText(this, "Đã đến giờ ngừng đọc", Toast.LENGTH_SHORT).show();
        };

        sleepTimerHandler.postDelayed(sleepTimerRunnable, minutes * 60L * 1000L);
        Toast.makeText(this, "Sẽ ngừng đọc sau " + minutes + " phút", Toast.LENGTH_SHORT).show();
    }

    private void cancelSleepTimer(boolean silent) {
        if (sleepTimerRunnable != null) {
            sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
            sleepTimerRunnable = null;
        }
        stopWhenChapterEnds = false;
        if (!silent) Toast.makeText(this, "Đã tắt hẹn giờ", Toast.LENGTH_SHORT).show();
    }

    private void showAudioSpeedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn tốc độ đọc");
        builder.setSingleChoiceItems(speechRateLabels, currentSpeechRateIndex, (dialog, which) -> {
            currentSpeechRateIndex = which;
            if (textToSpeech != null) textToSpeech.setSpeechRate(speechRates[currentSpeechRateIndex]);
            updateAudioSpeedText();
            dialog.dismiss();
            if (isAudioPlaying) speakCurrentPart();
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void updateAudioSpeedText() {
        if (txtAudioSpeed != null) txtAudioSpeed.setText("Tốc độ: " + speechRateLabels[currentSpeechRateIndex]);
    }

    private void updatePlayPauseIcon() {
        if (imgPlayPauseAudio == null) return;
        imgPlayPauseAudio.setImageResource(isAudioPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
    }

    private void updateAudioCurrentPartText() {
        if (txtAudioCurrentPart == null) return;
        if (audioParts.isEmpty()) {
            txtAudioCurrentPart.setText("Chưa có nội dung audio");
            return;
        }
        txtAudioCurrentPart.setText((isAudioPlaying ? "Đang đọc: đoạn " : "Đang chọn: đoạn ") + (currentAudioIndex + 1) + "/" + audioParts.size());
    }

    private void highlightCurrentAudioPart() {
        if (fullChapterContent == null || fullChapterContent.isEmpty() || !isHighlightEnabled || audioParts.isEmpty() || currentAudioIndex < 0 || currentAudioIndex >= audioParts.size()) {
            tvChapterContent.setText(fullChapterContent == null ? "" : fullChapterContent);
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

        db.collection("Users").document(uid).collection("Library")
                .whereEqualTo("bookId", bookId).whereEqualTo("type", "history")
                .limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) queryDocumentSnapshots.getDocuments().get(0).getReference().set(payload);
                    else db.collection("Users").document(uid).collection("Library").add(payload);
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveAudioReadingState();
    }

    @Override
    protected void onDestroy() {
        saveAudioReadingState();
        cancelSleepTimer(true);
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        super.onDestroy();
    }

    private List<String> splitIntoParagraphChunks(String text, int maxLength) {
        List<String> result = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\n+");
        for (String paragraph : paragraphs) {
            String cleaned = paragraph.trim();
            if (cleaned.isEmpty()) continue;
            if (cleaned.length() <= maxLength) result.add(cleaned);
            else result.addAll(splitLongTextBySentence(cleaned, maxLength));
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

            if (currentChunk.length() == 0) currentChunk.append(trimmed);
            else if (currentChunk.length() + 1 + trimmed.length() <= maxLength) currentChunk.append(" ").append(trimmed);
            else {
                result.add(currentChunk.toString().trim());
                currentChunk.setLength(0);
                currentChunk.append(trimmed);
            }
        }
        if (currentChunk.length() > 0) result.add(currentChunk.toString().trim());
        return result;
    }

    private List<String> splitVeryLongSentence(String text, int maxLength) {
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxLength, text.length());
            if (end < text.length()) {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > start) end = lastSpace;
            }
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) result.add(chunk);
            start = end;
        }
        return result;
    }

    public static class HistoryLibraryPayload {
        public String bookId;
        public String type;
        public String lastReadChapterId;
        public Timestamp timestamp;

        public HistoryLibraryPayload() {}

        public HistoryLibraryPayload(String bookId, String type, String lastReadChapterId) {
            this.bookId = bookId;
            this.type = type;
            this.lastReadChapterId = lastReadChapterId;
            this.timestamp = Timestamp.now();
        }
    }
}