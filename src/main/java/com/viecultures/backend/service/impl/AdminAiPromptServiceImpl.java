package com.viecultures.backend.service.impl;

import com.viecultures.backend.dto.admin.ai.AiPromptConfigDto;
import com.viecultures.backend.dto.admin.ai.AiPromptVariableDto;
import com.viecultures.backend.dto.admin.ai.UpdateAiPromptRequest;
import com.viecultures.backend.entity.SystemAiSetting;
import com.viecultures.backend.repository.SystemAiSettingRepository;
import com.viecultures.backend.service.AdminAiPromptService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAiPromptServiceImpl implements AdminAiPromptService {

    private final SystemAiSettingRepository systemAiSettingRepository;

    private static final String SETTING_KEY_PREFIX = "prompt_";

    @Getter
    private static class PromptDefinition {
        private final String key;
        private final String name;
        private final String description;
        private final String category;
        private final String defaultPrompt;
        private final List<AiPromptVariableDto> supportedVariables;

        public PromptDefinition(String key, String name, String description, String category,
                                String defaultPrompt, List<AiPromptVariableDto> supportedVariables) {
            this.key = key;
            this.name = name;
            this.description = description;
            this.category = category;
            this.defaultPrompt = defaultPrompt;
            this.supportedVariables = supportedVariables != null ? supportedVariables : Collections.emptyList();
        }
    }

    private static final Map<String, PromptDefinition> PROMPT_REGISTRY = new LinkedHashMap<>();

    static {
        register(new PromptDefinition(
                "companion_system_instruction",
                "Chỉ thị hội thoại Trợ lý Văn hóa (Companion)",
                "Persona, giọng điệu và phong cách giao tiếp tổng thể của AI bạn đồng hành VieCultures.",
                "COMPANION_CHAT",
                "You are VieCultures Cultural Companion, an authentic, warm, and engaging conversational partner passionate about Vietnamese culture, heritage, folklore, traditions, and cuisine.\n\n"
                        + "Your mission: Converse with users in natural, fluid, and encouraging tone. Speak like a knowledgeable local friend sharing a story over coffee, not an encyclopedia or formal assistant.\n\n"
                        + "{{groundedContext}}\n\n"
                        + "[NATURAL CONVERSATION PRINCIPLES]\n"
                        + "1. Be human and warm: Never use robotic AI filler or canned templates (e.g. 'I am delighted you are asking...', 'As an AI...', 'According to the database...', 'While our library is expanding...'). Jump right into the conversation with genuine warmth and enthusiasm.\n"
                        + "2. Avoid rigid formatting: Do not format responses with labeled blocks (e.g. '[Summary]:', '[Cultural Context]:'), raw asterisk dividers like '***', or heavy bullet lists unless specifically requested. Write smooth, vivid, well-paced paragraphs.\n"
                        + "3. Natural storytelling: Seamlessly integrate cultural knowledge from [CULTURAL CONTEXT ARCHIVE] into your dialogue. If the topic is broader, share accurate, captivating cultural insights with curiosity.\n"
                        + "4. Length: Keep replies concise and easy to read (around 80-150 words).\n"
                        + "5. Conversation prompts: End your response with exactly 2 thoughtful follow-up questions to keep the chat going, formatted as:\n"
                        + "PROMPTS:\n"
                        + "- <Question 1>\n"
                        + "- <Question 2>",
                List.of(
                        AiPromptVariableDto.builder()
                                .name("{{groundedContext}}")
                                .description("Nội dung tài liệu văn hóa và chỉ thị ngôn ngữ tương ứng được truy xuất tự động")
                                .build()
                )
        ));

        register(new PromptDefinition(
                "companion_lang_vi",
                "Chỉ thị ngôn ngữ Tiếng Việt (Companion)",
                "Quy tắc bắt buộc mô hình phản hồi hoàn toàn bằng tiếng Việt tự nhiên và ấm áp.",
                "COMPANION_CHAT",
                "[LANGUAGE REQUIREMENT]\n"
                        + "1. Strictly Vietnamese: You MUST reply entirely in natural, warm, and authentic Vietnamese (Tieng Viet).\n"
                        + "2. Do not respond in English unless quoting specific proper nouns or English terminology.\n"
                        + "3. The 2 follow-up questions in PROMPTS must be written in Vietnamese.\n\n",
                Collections.emptyList()
        ));

        register(new PromptDefinition(
                "companion_lang_en",
                "Chỉ thị ngôn ngữ Tiếng Anh (Companion)",
                "Quy tắc bắt buộc mô hình phản hồi bằng tiếng Anh thân thiện, dễ tiếp cận cho người học.",
                "COMPANION_CHAT",
                "[LANGUAGE REQUIREMENT]\n"
                        + "1. Strictly English: Always respond in natural, friendly English (accessible for CEFR A2-B2 learners).\n"
                        + "2. The 2 follow-up questions in PROMPTS must be written in English.\n\n",
                Collections.emptyList()
        ));

        register(new PromptDefinition(
                "companion_lang_bilingual",
                "Chỉ thị ngôn ngữ Song ngữ Anh - Việt (Companion)",
                "Quy tắc yêu cầu phản hồi song ngữ giải thích tiếng Anh kết hợp tiếng Việt song hành.",
                "COMPANION_CHAT",
                "[LANGUAGE REQUIREMENT]\n"
                        + "1. Bilingual Format: You MUST provide a rich bilingual response.\n"
                        + "2. First deliver the explanation in natural, warm English (accessible for CEFR A2-B2 learners).\n"
                        + "3. Next provide an authentic Vietnamese translation and cultural explanation (Tieng Viet) immediately following.\n"
                        + "4. The 2 follow-up questions in PROMPTS must include both English and Vietnamese translations.\n\n",
                Collections.emptyList()
        ));

        register(new PromptDefinition(
                "companion_translation_prompt",
                "Chỉ thị dịch thuật ngữ cảnh văn hóa",
                "Chỉ đạo Gemini dịch thuật chuẩn xác giữa Tiếng Anh và Tiếng Việt, giữ trọn sắc thái văn hóa và thuật ngữ đặc trưng.",
                "TRANSLATION",
                "You are an expert Vietnamese-English cultural translator for the VieCultures platform.\n"
                        + "Translate the user's text accurately between Vietnamese and English, paying close attention to cultural context, idioms, culinary vocabulary, and historical nuance.\n\n"
                        + "Guidelines:\n"
                        + "1. Source language: {{sourceLang}}, Target language: {{targetLang}}.\n"
                        + "2. If translating Vietnamese to English: produce natural, elegant, and vivid English. Preserve iconic cultural terms (like 'ao dai', 'banh chung', 'pho', 'tet') when culturally appropriate, accompanied by subtle context.\n"
                        + "3. If translating English to Vietnamese: produce fluid, idiomatic, and graceful Vietnamese, avoiding awkward literal word-for-word translation.\n"
                        + "4. Output ONLY the translated text. Do NOT include greetings, preamble, explanations, markdown fences, quotation marks, or meta commentary.",
                List.of(
                        AiPromptVariableDto.builder()
                                .name("{{sourceLang}}")
                                .description("Ngôn ngữ nguồn (ví dụ: Vietnamese hoặc English)")
                                .build(),
                        AiPromptVariableDto.builder()
                                .name("{{targetLang}}")
                                .description("Ngôn ngữ đích cần dịch sang")
                                .build()
                )
        ));

        register(new PromptDefinition(
                "stt_pass1_verbatim_prompt",
                "STT Pass 1 - Nhận dạng âm thanh thô",
                "Chỉ thị phiên âm chính xác từng từ phát âm (verbatim), giữ nguyên từ đệm và ngữ âm tự nhiên.",
                "STT",
                "Transcribe the spoken audio verbatim. STRICT RULE: This platform strictly supports ONLY English and Vietnamese. Output ONLY the exact spoken English words (or Vietnamese if spoken). Do NOT translate, do NOT output or guess words in any other foreign languages (e.g., Chinese, Japanese, Korean, Spanish, French). Do NOT omit filler words, stutters, repetitions, or grammatical errors; transcribe every single utterance verbatim. If the audio is silent or unintelligible noise, respond with EMPTY_SPEECH.",
                Collections.emptyList()
        ));

        register(new PromptDefinition(
                "stt_pass2_semantic_prompt",
                "STT Pass 2 - Biên tập & Tinh chỉnh ngữ nghĩa",
                "Chỉ thị tinh chỉnh ngữ pháp, loại bỏ từ ngập ngừng (fillers) và định dạng JSON phản hồi kèm danh sách lỗi.",
                "STT",
                "You are an expert English dialogue editor for language learners.\n"
                        + "STRICT LANGUAGE RULE: Only English and Vietnamese are permitted. Do NOT output any third languages.\n"
                        + "Input: verbatim raw transcript of an English learner.\n"
                        + "Tasks:\n"
                        + "1. Remove verbal fillers and hesitation (um, uh, er, ah, like, you know).\n"
                        + "2. Repair self-corrections/false starts (e.g. 'She go... I mean she goes' -> 'She goes').\n"
                        + "3. Fix context homophones, capitalization, and punctuation.\n"
                        + "4. Keep the learner's vocabulary level intact.\n"
                        + "Return strictly a valid JSON object:\n"
                        + "{\"cleaned_text\": \"...\", \"corrections\": [{\"original\": \"...\", \"replacement\": \"...\", \"type\": \"...\", \"explanation\": \"...\"}]}",
                Collections.emptyList()
        ));

        register(new PromptDefinition(
                "map_ai_system_prompt",
                "Chỉ thị Sứ giả văn hóa bản đồ",
                "Persona hướng dẫn viên văn hóa tương tác trên bản đồ di sản, giới thiệu di tích, lễ hội và phong vị địa phương.",
                "MAP_AI",
                "You are the VieCultures Map Cultural Guide, a friendly and knowledgeable local explorer navigating Vietnam's historical landmarks, cultural heritage sites, and traditional craft villages.\n"
                        + "Provide captivating, concise storytelling about each location, highlighting historical background, architectural marvels, local festivals, and must-try delicacies.\n"
                        + "Maintain an inviting, respectful, and culturally authentic perspective.",
                Collections.emptyList()
        ));
    }

    private static void register(PromptDefinition def) {
        PROMPT_REGISTRY.put(def.getKey(), def);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiPromptConfigDto> getAllPrompts() {
        List<AiPromptConfigDto> result = new ArrayList<>();
        for (PromptDefinition def : PROMPT_REGISTRY.values()) {
            result.add(buildPromptDto(def));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public AiPromptConfigDto getPromptByKey(String key) {
        PromptDefinition def = findDefinitionOrThrow(key);
        return buildPromptDto(def);
    }

    @Override
    @Transactional
    public AiPromptConfigDto updatePrompt(String key, UpdateAiPromptRequest request) {
        PromptDefinition def = findDefinitionOrThrow(key);
        String settingKey = SETTING_KEY_PREFIX + def.getKey();

        SystemAiSetting setting = systemAiSettingRepository.findBySettingKey(settingKey)
                .or(() -> systemAiSettingRepository.findBySettingKey(def.getKey()))
                .orElse(SystemAiSetting.builder()
                        .settingKey(settingKey)
                        .description("Tùy chỉnh prompt cho " + def.getName())
                        .build());

        setting.setSettingKey(settingKey);
        setting.setSettingValue(request.getPromptText().trim());
        setting.setDescription("Tùy chỉnh prompt cho " + def.getName());
        systemAiSettingRepository.save(setting);

        log.info("Da cap nhat System Prompt cho key: {}", key);
        return buildPromptDto(def);
    }

    @Override
    @Transactional
    public AiPromptConfigDto resetPrompt(String key) {
        PromptDefinition def = findDefinitionOrThrow(key);
        String settingKey = SETTING_KEY_PREFIX + def.getKey();

        systemAiSettingRepository.findBySettingKey(settingKey)
                .ifPresent(systemAiSettingRepository::delete);
        systemAiSettingRepository.findBySettingKey(def.getKey())
                .ifPresent(systemAiSettingRepository::delete);

        log.info("Da reset System Prompt ve mac dinh cho key: {}", key);
        return buildPromptDto(def);
    }

    @Override
    @Transactional(readOnly = true)
    public String getResolvedPrompt(String key, String defaultFallback) {
        if (key == null || key.isBlank()) {
            return defaultFallback;
        }

        String settingKey = SETTING_KEY_PREFIX + key.trim();
        Optional<SystemAiSetting> settingOpt = systemAiSettingRepository.findBySettingKey(settingKey)
                .or(() -> systemAiSettingRepository.findBySettingKey(key.trim()));

        if (settingOpt.isPresent()) {
            String val = settingOpt.get().getSettingValue();
            if (val != null && !val.isBlank()) {
                return val.trim();
            }
        }

        PromptDefinition def = PROMPT_REGISTRY.get(key.trim());
        if (def != null && def.getDefaultPrompt() != null && !def.getDefaultPrompt().isBlank()) {
            return def.getDefaultPrompt();
        }

        return defaultFallback;
    }

    private PromptDefinition findDefinitionOrThrow(String key) {
        if (key == null || !PROMPT_REGISTRY.containsKey(key.trim())) {
            throw new IllegalArgumentException("Khong tim thay cau hinh prompt voi key: " + key);
        }
        return PROMPT_REGISTRY.get(key.trim());
    }

    private AiPromptConfigDto buildPromptDto(PromptDefinition def) {
        String settingKey = SETTING_KEY_PREFIX + def.getKey();
        Optional<SystemAiSetting> settingOpt = systemAiSettingRepository.findBySettingKey(settingKey)
                .or(() -> systemAiSettingRepository.findBySettingKey(def.getKey()));

        boolean isModified = false;
        String currentPrompt = def.getDefaultPrompt();
        java.time.LocalDateTime updatedAt = null;

        if (settingOpt.isPresent()) {
            SystemAiSetting setting = settingOpt.get();
            if (setting.getSettingValue() != null && !setting.getSettingValue().isBlank()) {
                currentPrompt = setting.getSettingValue();
                isModified = true;
                updatedAt = setting.getUpdatedAt();
            }
        }

        return AiPromptConfigDto.builder()
                .key(def.getKey())
                .name(def.getName())
                .description(def.getDescription())
                .category(def.getCategory())
                .currentPrompt(currentPrompt)
                .defaultPrompt(def.getDefaultPrompt())
                .isModified(isModified)
                .supportedVariables(def.getSupportedVariables())
                .updatedAt(updatedAt)
                .build();
    }
}
