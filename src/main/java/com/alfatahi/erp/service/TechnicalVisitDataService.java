package com.alfatahi.erp.service;

import com.alfatahi.erp.dto.TechnicalVisitDetailsDto;
import com.alfatahi.erp.dto.TechnicalVisitOpeningRequest;
import com.alfatahi.erp.dto.TechnicalVisitSaveRequest;
import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.repository.ClientRepository;
import com.alfatahi.erp.repository.TechnicalVisitOpeningRepository;
import com.alfatahi.erp.repository.TechnicalVisitPhotoRepository;
import com.alfatahi.erp.repository.TechnicalVisitRepository;
import com.alfatahi.erp.planocorte.entity.ReferenciaHorizontal;
import com.alfatahi.erp.planocorte.entity.ReferenciaVertical;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class TechnicalVisitDataService {
    private static final long MAX_PHOTO_BYTES = 12L * 1024 * 1024;
    private static final long MAX_STORED_PHOTO_BYTES = 2L * 1024 * 1024;
    private static final int MAX_PHOTO_DIMENSION = 1920;
    private static final long MAX_PHOTO_PIXELS = 40_000_000L;
    private static final Set<String> VALID_STATUSES = Set.of("AGENDADA", "EM_ANDAMENTO", "CONCLUIDA");

    private final TechnicalVisitRepository visitRepository;
    private final ClientRepository clientRepository;
    private final TechnicalVisitOpeningRepository openingRepository;
    private final TechnicalVisitPhotoRepository photoRepository;

    public TechnicalVisitDataService(TechnicalVisitRepository visitRepository,
                                     ClientRepository clientRepository,
                                     TechnicalVisitOpeningRepository openingRepository,
                                     TechnicalVisitPhotoRepository photoRepository) {
        this.visitRepository = visitRepository;
        this.clientRepository = clientRepository;
        this.openingRepository = openingRepository;
        this.photoRepository = photoRepository;
    }

    public UUID createForClient(TechnicalVisitSaveRequest request) {
        if (request.getClientId() == null) throw new IllegalArgumentException("Selecione o cliente.");
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new NoSuchElementException("Cliente não encontrado."));
        TechnicalVisit visit = new TechnicalVisit();
        visit.setClient(client);
        visit.setVisitDate(request.getVisitDate() != null ? request.getVisitDate() : LocalDate.now());
        visit.setVisitTime(request.getVisitTime());
        visit.setNotes(request.getNotes());
        visit.setStatus(normalizeStatus(request.getStatus()));
        return visitRepository.saveAndFlush(visit).getId();
    }

    public void updateVisit(UUID visitId, TechnicalVisitSaveRequest request) {
        TechnicalVisit visit = findVisit(visitId);
        if (request.getVisitDate() != null) visit.setVisitDate(request.getVisitDate());
        visit.setVisitTime(request.getVisitTime());
        visit.setNotes(request.getNotes());
        visit.setStatus(normalizeStatus(request.getStatus()));
        visitRepository.save(visit);
    }

    public TechnicalVisitOpening saveOpening(UUID visitId, TechnicalVisitOpeningRequest request) {
        TechnicalVisit visit = findVisit(visitId);
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Informe um nome para o vão, por exemplo: Box do banheiro.");
        }
        if (request.getServiceCategory() == null) {
            throw new IllegalArgumentException("Selecione o tipo de serviço desta medição.");
        }
        validateDimensions(request);

        TechnicalVisitOpening opening = request.getId() == null
                ? new TechnicalVisitOpening()
                : openingRepository.findByIdAndTechnicalVisitId(request.getId(), visitId)
                    .orElseThrow(() -> new NoSuchElementException("Vão não encontrado nesta visita."));
        opening.setTechnicalVisit(visit);
        opening.setName(request.getName().trim());
        opening.setServiceCategory(request.getServiceCategory());
        opening.setWidthMm(request.getWidthMm());
        opening.setHeightMm(request.getHeightMm());
        opening.setGrossHeightLeftMm(request.getGrossHeightLeftMm());
        opening.setGrossHeightRightMm(request.getGrossHeightRightMm());
        opening.setGrossWidthTopMm(request.getGrossWidthTopMm());
        opening.setGrossWidthBottomMm(request.getGrossWidthBottomMm());
        opening.setNotes(request.getNotes());
        opening.getFeatures().clear();

        for (TechnicalVisitOpeningRequest.FeatureRequest f : request.getFeatures()) {
            if (f.getType() == null) throw new IllegalArgumentException("Selecione o tipo de cada furo/recorte.");
            TechnicalVisitFeature feature = new TechnicalVisitFeature();
            feature.setOpening(opening);
            feature.setType(f.getType());
            feature.setName(trimToNull(f.getName()));
            feature.setReferenceHorizontal(f.getReferenceHorizontal() != null ? f.getReferenceHorizontal() : ReferenciaHorizontal.ESQUERDA);
            feature.setDistanceHorizontalMm(nonNegative(f.getDistanceHorizontalMm(), "Distância horizontal"));
            feature.setReferenceVertical(f.getReferenceVertical() != null ? f.getReferenceVertical() : ReferenciaVertical.SUPERIOR);
            feature.setDistanceVerticalMm(nonNegative(f.getDistanceVerticalMm(), "Distância vertical"));
            feature.setDiameterMm(nonNegative(f.getDiameterMm(), "Diâmetro"));
            feature.setWidthMm(nonNegative(f.getWidthMm(), "Largura do recorte"));
            feature.setHeightMm(nonNegative(f.getHeightMm(), "Altura do recorte"));
            feature.setDepthMm(nonNegative(f.getDepthMm(), "Profundidade"));
            feature.setRadiusMm(nonNegative(f.getRadiusMm(), "Raio"));
            feature.setCorner(trimToNull(f.getCorner()));
            feature.setNotes(trimToNull(f.getNotes()));
            opening.getFeatures().add(feature);
        }
        return openingRepository.saveAndFlush(opening);
    }

    public void deleteOpening(UUID visitId, UUID openingId) {
        TechnicalVisitOpening opening = openingRepository.findByIdAndTechnicalVisitId(openingId, visitId)
                .orElseThrow(() -> new NoSuchElementException("Vão não encontrado nesta visita."));
        openingRepository.delete(opening);
    }

    public UUID addPhoto(UUID visitId, MultipartFile file, String caption) throws IOException {
        TechnicalVisit visit = findVisit(visitId);
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Selecione uma foto.");
        if (file.getSize() > MAX_PHOTO_BYTES) throw new IllegalArgumentException("A foto deve ter no máximo 12 MB.");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("O arquivo enviado precisa ser uma imagem.");
        }
        ProcessedPhoto processed = processPhoto(file);
        TechnicalVisitPhoto photo = new TechnicalVisitPhoto();
        photo.setTechnicalVisit(visit);
        photo.setFileName(optimizedFileName(file.getOriginalFilename()));
        photo.setContentType(processed.contentType());
        photo.setFileSize((long) processed.content().length);
        photo.setCaption(trimToNull(caption));
        photo.setContent(processed.content());
        return photoRepository.saveAndFlush(photo).getId();
    }

    @Transactional(readOnly = true)
    public TechnicalVisitPhoto getPhoto(UUID visitId, UUID photoId) {
        return photoRepository.findByIdAndTechnicalVisitId(photoId, visitId)
                .orElseThrow(() -> new NoSuchElementException("Foto não encontrada nesta visita."));
    }

    public void deletePhoto(UUID visitId, UUID photoId) {
        photoRepository.delete(getPhoto(visitId, photoId));
    }

    @Transactional(readOnly = true)
    public List<TechnicalVisitDetailsDto> listAllDetailed() {
        return visitRepository.findAllWithRelations().stream().map(this::toDetails).toList();
    }

    @Transactional(readOnly = true)
    public TechnicalVisitDetailsDto findDetailed(UUID visitId) { return toDetails(findVisit(visitId)); }

    @Transactional(readOnly = true)
    public List<TechnicalVisitOpening> findOpeningsForClient(UUID clientId) {
        return openingRepository.findDetailedByClientId(clientId);
    }

    @Transactional(readOnly = true)
    public TechnicalVisitOpening findOpening(UUID openingId) {
        return openingRepository.findById(openingId)
                .orElseThrow(() -> new NoSuchElementException("Medição de vão não encontrada."));
    }

    private TechnicalVisitDetailsDto toDetails(TechnicalVisit visit) {
        Client client = visit.getClient();
        String address = client == null ? "Endereço não informado" : joinAddress(client.getAddress(), client.getCity());
        List<TechnicalVisitDetailsDto.OpeningDto> openings = openingRepository.findDetailedByVisitId(visit.getId()).stream()
                .map(o -> new TechnicalVisitDetailsDto.OpeningDto(o.getId(), o.getServiceCategory(), o.getName(), o.getWidthMm(), o.getHeightMm(),
                        o.getGrossHeightLeftMm(), o.getGrossHeightRightMm(), o.getGrossWidthTopMm(), o.getGrossWidthBottomMm(),
                        o.getNotes(), o.getFeatures().stream().map(f -> new TechnicalVisitDetailsDto.FeatureDto(
                                f.getId(), f.getType(), f.getName(), f.getReferenceHorizontal(), f.getDistanceHorizontalMm(),
                                f.getReferenceVertical(), f.getDistanceVerticalMm(), f.getDiameterMm(), f.getWidthMm(),
                                f.getHeightMm(), f.getDepthMm(), f.getRadiusMm(), f.getCorner(), f.getNotes())).toList())).toList();
        List<TechnicalVisitDetailsDto.PhotoDto> photos = photoRepository.findByTechnicalVisitIdOrderByCreatedAtAsc(visit.getId()).stream()
                .map(p -> new TechnicalVisitDetailsDto.PhotoDto(p.getId(), p.getFileName(), p.getContentType(), p.getFileSize(), p.getCaption())).toList();
        return new TechnicalVisitDetailsDto(visit.getId(), client != null ? client.getId() : null,
                client != null ? client.getName() : "Cliente não informado", address,
                visit.getQuote() != null ? visit.getQuote().getId() : null,
                visit.getQuote() != null ? visit.getQuote().getNumber() : null,
                visit.getVisitDate(), visit.getVisitTime(), visit.getNotes(), visit.getStatus(), openings, photos);
    }

    private ProcessedPhoto processPhoto(MultipartFile file) throws IOException {
        byte[] original = file.getBytes();
        BufferedImage source;
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(original))) {
            if (input == null) {
                throw new IllegalArgumentException("Nao foi possivel ler a imagem enviada.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Formato de imagem nao suportado. Use JPG ou PNG.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PHOTO_PIXELS) {
                    throw new IllegalArgumentException("A resolucao da foto e muito alta.");
                }
                source = reader.read(0);
            } finally {
                reader.dispose();
            }
        }

        if (source == null) {
            throw new IllegalArgumentException("O arquivo enviado nao contem uma imagem valida.");
        }
        double scale = Math.min(1d, (double) MAX_PHOTO_DIMENSION / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage current = resizeToRgb(source, width, height);
        byte[] encoded = encodeJpeg(current, 0.82f);

        while (encoded.length > MAX_STORED_PHOTO_BYTES && Math.max(current.getWidth(), current.getHeight()) > 960) {
            int nextWidth = Math.max(1, (int) Math.round(current.getWidth() * 0.82));
            int nextHeight = Math.max(1, (int) Math.round(current.getHeight() * 0.82));
            current = resizeToRgb(current, nextWidth, nextHeight);
            encoded = encodeJpeg(current, 0.76f);
        }
        for (float quality = 0.68f; encoded.length > MAX_STORED_PHOTO_BYTES && quality >= 0.48f; quality -= 0.10f) {
            encoded = encodeJpeg(current, quality);
        }
        if (encoded.length > MAX_STORED_PHOTO_BYTES) {
            throw new IllegalArgumentException("Nao foi possivel otimizar a foto para menos de 2 MB.");
        }
        return new ProcessedPhoto(encoded, "image/jpeg");
    }

    private BufferedImage resizeToRgb(BufferedImage source, int width, int height) {
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private byte[] encodeJpeg(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("Codificador JPEG indisponivel.");
        }
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam params = writer.getDefaultWriteParam();
            if (params.canWriteCompressed()) {
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(Math.max(0.1f, Math.min(1f, quality)));
            }
            writer.write(null, new IIOImage(image, null, null), params);
            imageOutput.flush();
            return output.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private String optimizedFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "foto-visita.jpg";
        }
        int slash = Math.max(originalName.lastIndexOf('/'), originalName.lastIndexOf('\\'));
        String baseName = slash >= 0 ? originalName.substring(slash + 1) : originalName;
        int extension = baseName.lastIndexOf('.');
        if (extension > 0) {
            baseName = baseName.substring(0, extension);
        }
        return (baseName.isBlank() ? "foto-visita" : baseName) + ".jpg";
    }

    private record ProcessedPhoto(byte[] content, String contentType) {
    }

    private TechnicalVisit findVisit(UUID id) {
        return visitRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Visita técnica não encontrada."));
    }

    private void validateDimensions(TechnicalVisitOpeningRequest r) {
        boolean simple = positive(r.getWidthMm()) && positive(r.getHeightMm());
        boolean detailed = nonNegativePresent(r.getGrossHeightLeftMm()) && nonNegativePresent(r.getGrossHeightRightMm())
                && nonNegativePresent(r.getGrossWidthTopMm()) && nonNegativePresent(r.getGrossWidthBottomMm())
                && r.getGrossHeightLeftMm().max(r.getGrossHeightRightMm()).signum() > 0
                && r.getGrossWidthTopMm().max(r.getGrossWidthBottomMm()).signum() > 0;
        if (!simple && !detailed) {
            throw new IllegalArgumentException("Informe largura e altura ou as quatro medidas brutas detalhadas do vão.");
        }
    }

    private boolean positive(BigDecimal value) { return value != null && value.signum() > 0; }
    private boolean nonNegativePresent(BigDecimal value) { return value != null && value.signum() >= 0; }
    private BigDecimal nonNegative(BigDecimal value, String field) {
        if (value != null && value.signum() < 0) throw new IllegalArgumentException(field + " não pode ser negativa.");
        return value;
    }
    private String normalizeStatus(String value) {
        String normalized = value == null || value.isBlank() ? "AGENDADA" : value.toUpperCase();
        if (!VALID_STATUSES.contains(normalized)) throw new IllegalArgumentException("Status da visita inválido.");
        return normalized;
    }
    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String joinAddress(String address, String city) {
        if (address != null && !address.isBlank() && city != null && !city.isBlank()) return address + " - " + city;
        if (address != null && !address.isBlank()) return address;
        if (city != null && !city.isBlank()) return city;
        return "Endereço não informado";
    }
}
