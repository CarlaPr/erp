package com.alfatahi.erp.controller;

import com.alfatahi.erp.dto.TechnicalVisitOpeningRequest;
import com.alfatahi.erp.dto.TechnicalVisitSaveRequest;
import com.alfatahi.erp.entity.TechnicalVisitPhoto;
import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.alfatahi.erp.repository.ClientRepository;
import com.alfatahi.erp.service.TechnicalVisitDataService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/technical-visits")
public class TechnicalVisitPageController {
    private final TechnicalVisitDataService dataService;
    private final ClientRepository clientRepository;

    public TechnicalVisitPageController(TechnicalVisitDataService dataService, ClientRepository clientRepository) {
        this.dataService = dataService;
        this.clientRepository = clientRepository;
    }

    @GetMapping
    public String page(@RequestParam(required = false) UUID visit, Model model) {
        model.addAttribute("currentPage", "technical-visits");
        model.addAttribute("clients", clientRepository.findSelectableClients());
        model.addAttribute("serviceCategories", CategoriaServico.values());
        model.addAttribute("visits", dataService.listAllDetailed());
        model.addAttribute("selectedVisitId", visit);
        return "technical-visits";
    }

    @PostMapping(value = "/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> create(@RequestBody TechnicalVisitSaveRequest request) {
        try { return ok("id", dataService.createForClient(request)); }
        catch (RuntimeException e) { return bad(e); }
    }

    @PostMapping(value = "/{visitId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> update(@PathVariable UUID visitId,
                                                       @RequestBody TechnicalVisitSaveRequest request) {
        try { dataService.updateVisit(visitId, request); return ok("ok", true); }
        catch (RuntimeException e) { return bad(e); }
    }

    @PostMapping(value = "/{visitId}/openings", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveOpening(@PathVariable UUID visitId,
                                                            @RequestBody TechnicalVisitOpeningRequest request) {
        try { return ok("id", dataService.saveOpening(visitId, request).getId()); }
        catch (RuntimeException e) { return bad(e); }
    }

    @DeleteMapping("/{visitId}/openings/{openingId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteOpening(@PathVariable UUID visitId, @PathVariable UUID openingId) {
        try { dataService.deleteOpening(visitId, openingId); return ok("ok", true); }
        catch (RuntimeException e) { return bad(e); }
    }

    @PostMapping(value = "/{visitId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadPhoto(@PathVariable UUID visitId,
                                                            @RequestPart("file") MultipartFile file,
                                                            @RequestParam(required = false) String caption,
                                                            @RequestParam(required = false) UUID openingId) {
        try { return ok("id", dataService.addMedia(visitId, file, caption, openingId)); }
        catch (Exception e) { return bad(e); }
    }

    @GetMapping("/{visitId}/photos/{photoId}")
    public ResponseEntity<byte[]> photo(@PathVariable UUID visitId, @PathVariable UUID photoId,
                                        @RequestHeader(value = HttpHeaders.RANGE, required = false) String range) {
        TechnicalVisitPhoto photo = dataService.getPhoto(visitId, photoId);
        MediaType mediaType;
        try { mediaType = MediaType.parseMediaType(photo.getContentType()); }
        catch (Exception ignored) { mediaType = MediaType.APPLICATION_OCTET_STREAM; }
        byte[] content = photo.getContent();
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (range != null && range.startsWith("bytes=") && content.length > 0) {
            String[] bounds = range.substring(6).split("-", 2);
            try {
                int start = Integer.parseInt(bounds[0]);
                int end = bounds.length > 1 && !bounds[1].isBlank()
                        ? Math.min(Integer.parseInt(bounds[1]), content.length - 1)
                        : Math.min(start + 1024 * 1024 - 1, content.length - 1);
                if (start >= 0 && start <= end && start < content.length) {
                    content = java.util.Arrays.copyOfRange(content, start, end + 1);
                    response = ResponseEntity.status(206)
                            .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + photo.getContent().length);
                }
            } catch (NumberFormatException ignored) { }
        }
        return response.cacheControl(CacheControl.noCache()).contentType(mediaType)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .contentLength(content.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + photo.getFileName().replace("\"", "") + "\"")
                .body(content);
    }

    @DeleteMapping("/{visitId}/photos/{photoId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deletePhoto(@PathVariable UUID visitId, @PathVariable UUID photoId) {
        try { dataService.deletePhoto(visitId, photoId); return ok("ok", true); }
        catch (RuntimeException e) { return bad(e); }
    }

    private ResponseEntity<Map<String, Object>> ok(String key, Object value) {
        Map<String, Object> body = new LinkedHashMap<>(); body.put("ok", true); body.put(key, value);
        return ResponseEntity.ok(body);
    }
    private ResponseEntity<Map<String, Object>> bad(Exception e) {
        Map<String, Object> body = new LinkedHashMap<>(); body.put("ok", false); body.put("error", e.getMessage());
        return ResponseEntity.badRequest().body(body);
    }
}
