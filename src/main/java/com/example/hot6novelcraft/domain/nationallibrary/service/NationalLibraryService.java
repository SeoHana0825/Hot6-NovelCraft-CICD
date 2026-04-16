package com.example.hot6novelcraft.domain.nationallibrary.service;

import com.example.hot6novelcraft.common.dto.PageResponse;
import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.common.exception.domain.NationalLibraryExceptionEnum;
import com.example.hot6novelcraft.domain.nationallibrary.dto.request.BookSaveRequest;
import com.example.hot6novelcraft.domain.nationallibrary.dto.request.BookSearchRequest;
import com.example.hot6novelcraft.domain.nationallibrary.dto.request.UserBookSaveRequest;
import com.example.hot6novelcraft.domain.nationallibrary.dto.response.*;
import com.example.hot6novelcraft.domain.nationallibrary.entity.Book;
import com.example.hot6novelcraft.domain.nationallibrary.entity.UserBook;
import com.example.hot6novelcraft.domain.nationallibrary.infrastructure.NationalLibraryApiClient;
import com.example.hot6novelcraft.domain.nationallibrary.repository.BookRepository;
import com.example.hot6novelcraft.domain.nationallibrary.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NationalLibraryService {

    private final NationalLibraryApiClient apiClient;
    private final BookRepository bookRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserBookRepository  userBookRepository;

    private static final String SEARCH_CACHE_PREFIX = "book:search:";
    private static final Duration SEARCH_CACHE_TTL  = Duration.ofMinutes(10);

    // 도서 검색 (Redis 캐싱 적용)
    public PageResponse<NationalLibraryBookResponse> searchBooks(BookSearchRequest request) {
        String cacheKey = SEARCH_CACHE_PREFIX + request.query()
                + ":" + request.page() + ":" + request.size();

        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof PageResponse<?> pageResponse) {
            log.debug("캐시 히트 - key: {}", cacheKey);
            @SuppressWarnings("unchecked")
            PageResponse<NationalLibraryBookResponse> result =
                    (PageResponse<NationalLibraryBookResponse>) pageResponse;
            return result;
        }

        List<NationalLibraryApiItem> items =
                apiClient.searchBooks(request.query(), request.page(), request.size());

        List<NationalLibraryBookResponse> content = items.stream()
                .map(NationalLibraryBookResponse::from)
                .toList();

        PageResponse<NationalLibraryBookResponse> result = new PageResponse<>(
                content,
                request.page() - 1,
                (int) Math.ceil((double) content.size() / request.size()),
                content.size(),
                request.size(),
                content.size() < request.size()
        );

        redisTemplate.opsForValue().set(cacheKey, result, SEARCH_CACHE_TTL);
        return result;
    }

    // 도서 저장
    @Transactional
    public BookResponse saveBook(BookSaveRequest request) {
        if (bookRepository.existsByIsbn(request.isbn())) {
            throw new ServiceErrorException(NationalLibraryExceptionEnum.BOOK_ALREADY_EXISTS);
        }
        Book book = Book.from(request);
        return BookResponse.from(bookRepository.save(book));
    }

    // 도서 단건 조회
    @Transactional(readOnly = true)
    public BookResponse getBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() ->
                        new ServiceErrorException(NationalLibraryExceptionEnum.BOOK_NOT_FOUND));
        return BookResponse.from(book);
    }
    // 내 서재에 도서 저장
    @Transactional
    public UserBookResponse saveUserBook(Long userId, UserBookSaveRequest request) {

        Book book = bookRepository.findByIsbn(request.isbn())
                .orElseGet(() -> bookRepository.save(
                        Book.from(new BookSaveRequest(
                                request.isbn(),
                                request.title(),
                                request.author(),
                                request.publisher(),
                                request.publishYear(),
                                request.coverImageUrl()   // ← 추가
                        ))
                ));

        if (userBookRepository.existsByUserIdAndBookId(userId, book.getId())) {
            throw new ServiceErrorException(NationalLibraryExceptionEnum.BOOK_ALREADY_IN_SHELF);
        }

        UserBook userBook = userBookRepository.save(UserBook.of(userId, book.getId()));

        return UserBookResponse.of(userBook, book);
    }
    @Transactional(readOnly = true)
    public List<MyShelfResponse> getMyShelf(Long userId) {

        List<UserBook> userBooks = userBookRepository.findAllByUserId(userId);

        List<Long> bookIds = userBooks.stream()
                .map(UserBook::getBookId)
                .toList();

        Map<Long, Book> bookMap = bookRepository.findAllById(bookIds).stream()
                .collect(Collectors.toMap(Book::getId, book -> book));

        return userBooks.stream()
                .map(userBook -> MyShelfResponse.of(userBook, bookMap.get(userBook.getBookId())))
                .toList();
    }
}