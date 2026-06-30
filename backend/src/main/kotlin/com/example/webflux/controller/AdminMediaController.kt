package com.example.webflux.controller

import com.example.webflux.controller.model.AdminMediaDto
import com.example.webflux.controller.model.MediaReconcileRequest
import com.example.webflux.service.MediaService
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/goods/{goodsId}/media")
@PreAuthorize("hasRole('ADMIN')")
class AdminMediaController(
    private val mediaService: MediaService
) {
    /** Список медиа товара (с id — для удаления/порядка на клиенте). */
    @GetMapping
    suspend fun list(@PathVariable goodsId: String): List<AdminMediaDto> =
        mediaService.listByGoods(goodsId)

    /** Загрузка фото-файла (сжатый JPEG) → media + media_content. */
    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun upload(
        @PathVariable goodsId: String,
        @RequestPart("file") file: FilePart
    ): AdminMediaDto = mediaService.uploadPhoto(goodsId, file)

    /** Привести набор медиа к присланному упорядоченному списку. */
    @PutMapping
    suspend fun reconcile(
        @PathVariable goodsId: String,
        @RequestBody request: MediaReconcileRequest
    ): List<AdminMediaDto> = mediaService.reconcile(goodsId, request.items)
}
