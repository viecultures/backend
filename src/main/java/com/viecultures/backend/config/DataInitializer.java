package com.viecultures.backend.config;

import com.viecultures.backend.dto.ReadingPostRequest;
import com.viecultures.backend.entity.CefrLevel;
import com.viecultures.backend.repository.ReadingPostRepository;
import com.viecultures.backend.service.ReadingPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ReadingPostRepository readingPostRepository;
    private final ReadingPostService readingPostService;

    @Override
    public void run(String... args) {
        if (readingPostRepository.count() == 0) {
            log.info("[DataInitializer] Database chưa có bài đọc, tiến hành khởi tạo 2 bài đọc mẫu (Đông Hồ & Áo Dài)...");

            // 1. Bài 1: Tranh Đông Hồ
            ReadingPostRequest post1 = ReadingPostRequest.builder()
                    .titleEn("Dong Ho Folk Woodcut Paintings: The Soul of Vietnamese Traditional Art")
                    .titleVi("Tranh Dân Gian Đông Hồ: Hồn Cốt Mỹ Thuật Mộc Bản Dân Tộc")
                    .topic("festivals")
                    .level(CefrLevel.B2)
                    .readTime("4 phút")
                    .summary("Nét khắc than củi đanh gọn, màu khoáng từ điệp sò, hoa hòe và tro lá tre tạo nên bức tranh dân gian tràn đầy sức sống.")
                    .paragraphs(List.of(
                            ReadingPostRequest.ParagraphPairDto.builder()
                                    .orderIndex(0)
                                    .enText("Originating from Bac Ninh province, Dong Ho folk woodcut paintings embody the quintessence of Vietnamese rural philosophy and cultural aesthetics. Each artwork is printed manually on Dzo paper coated with a shimmering layer of crushed scallop shells known as Diep paper.")
                                    .viText("Bắt nguồn từ làng tranh Đông Hồ tỉnh Bắc Ninh, tranh khắc gỗ dân gian là hiện thân của tinh hoa triết lý nông thôn và mỹ thuật truyền thống Việt Nam. Mỗi bức tranh được in thủ công trên giấy Dó quét lớp bột vỏ sò điệp lấp lánh.")
                                    .audioTimestampStart(0.0f)
                                    .audioTimestampEnd(12.5f)
                                    .build(),
                            ReadingPostRequest.ParagraphPairDto.builder()
                                    .orderIndex(1)
                                    .enText("The artisans rely entirely on natural mineral pigments: soot from burned bamboo leaves for deep black outlines, gardenia seeds and sophora flowers for radiant yellow, and verdigris rust for antique jade green.")
                                    .viText("Các nghệ nhân hoàn toàn sử dụng phẩm màu tự nhiên từ khoáng vật: than tro lá tre cho nét viền đen than đanh gọn, hạt dành dành và hoa hòe cho sắc vàng rực rỡ, và gỉ đồng cho sắc xanh ngọc cổ kính.")
                                    .audioTimestampStart(12.5f)
                                    .audioTimestampEnd(25.0f)
                                    .build()
                    ))
                    .vocabularies(List.of(
                            ReadingPostRequest.VocabItemDto.builder()
                                    .word("quintessence")
                                    .ipa("/kwɪnˈtesns/")
                                    .pos("noun")
                                    .viMeaning("Tinh hoa, phần tinh túy nhất của một nền văn hóa")
                                    .contextSentence("Dong Ho folk woodcut paintings embody the quintessence of Vietnamese rural philosophy.")
                                    .level(CefrLevel.C1)
                                    .usageNote("Thường dùng trong văn viết học thuật: the quintessence of Vietnamese culture.")
                                    .build(),
                            ReadingPostRequest.VocabItemDto.builder()
                                    .word("aesthetics")
                                    .ipa("/iːsˈθetɪks/")
                                    .pos("noun")
                                    .viMeaning("Mỹ học, tính thẩm mỹ")
                                    .contextSentence("Embody the quintessence of Vietnamese rural philosophy and cultural aesthetics.")
                                    .level(CefrLevel.B2)
                                    .usageNote("Thuộc tính cảm thụ vẻ đẹp nghệ thuật.")
                                    .build()
                    ))
                    .build();

            // 2. Bài 2: Áo Dài Việt Nam
            ReadingPostRequest post2 = ReadingPostRequest.builder()
                    .titleEn("The Timeless Grace of the Vietnamese Ao Dai")
                    .titleVi("Nét Duyên Dáng Vượt Thời Gian Của Chiếc Áo Dài Việt Nam")
                    .topic("traditions")
                    .level(CefrLevel.B1)
                    .readTime("3 phút")
                    .summary("Biểu tượng của nét đẹp duyên dáng, kín đáo nhưng vẫn tôn vinh sự thanh lịch của người phụ nữ Việt Nam qua từng thời kỳ lịch sử.")
                    .paragraphs(List.of(
                            ReadingPostRequest.ParagraphPairDto.builder()
                                    .orderIndex(0)
                                    .enText("The Ao Dai is more than just a traditional garment; it is an enduring cultural symbol of Vietnamese elegance, modesty, and grace.")
                                    .viText("Áo Dài không chỉ đơn thuần là một bộ trang phục truyền thống; đó là một biểu tượng văn hóa bất hủ về nét thanh lịch, kín đáo và duyên dáng của người Việt.")
                                    .audioTimestampStart(0.0f)
                                    .audioTimestampEnd(8.0f)
                                    .build(),
                            ReadingPostRequest.ParagraphPairDto.builder()
                                    .orderIndex(1)
                                    .enText("With its split tunic worn over silk trousers, the dress blends ancient modesty with modern sophistication, making it celebrated across the world.")
                                    .viText("Với tà áo xẻ tà thướt tha mặc cùng quần lụa, chiếc áo kết hợp sự kín đáo cổ điển cùng vẻ đẹp tinh tế hiện đại, được tôn vinh trên toàn thế giới.")
                                    .audioTimestampStart(8.0f)
                                    .audioTimestampEnd(18.0f)
                                    .build()
                    ))
                    .vocabularies(List.of(
                            ReadingPostRequest.VocabItemDto.builder()
                                    .word("enduring")
                                    .ipa("/ɪnˈdjʊərɪŋ/")
                                    .pos("adjective")
                                    .viMeaning("Bền bỉ, lâu dài, trường tồn")
                                    .contextSentence("It is an enduring cultural symbol of Vietnamese elegance.")
                                    .level(CefrLevel.B2)
                                    .usageNote("Thường dùng để mô tả truyền thống hoặc giá trị bền vững.")
                                    .build(),
                            ReadingPostRequest.VocabItemDto.builder()
                                    .word("sophistication")
                                    .ipa("/səˌfɪstɪˈkeɪʃn/")
                                    .pos("noun")
                                    .viMeaning("Sự tinh tế, sự trang nhã hiện đại")
                                    .contextSentence("The dress blends ancient modesty with modern sophistication.")
                                    .level(CefrLevel.B2)
                                    .usageNote("Miêu tả phong cách tao nhã, có chiều sâu.")
                                    .build()
                    ))
                    .build();

            readingPostService.createPost(post1);
            readingPostService.createPost(post2);
            log.info("[DataInitializer] Đã khởi tạo thành công 2 bài đọc ảo vào cơ sở dữ liệu!");
        }
    }
}
