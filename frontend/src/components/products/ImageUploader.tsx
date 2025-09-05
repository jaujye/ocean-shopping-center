import React, { useState, useCallback, useRef } from 'react';
import { 
  PhotoIcon, 
  XMarkIcon, 
  ArrowUpTrayIcon,
  ExclamationTriangleIcon 
} from '@heroicons/react/24/outline';
import { cn } from '../../utils/cn';
import Button from '../ui/Button';
import { ProductImageUpload } from '../../types/product';
import { productService } from '../../services/productService';

interface ImageUploaderProps {
  images: string[];
  onImagesChange: (images: string[]) => void;
  maxImages?: number;
  maxFileSize?: number; // in MB
  acceptedTypes?: string[];
  disabled?: boolean;
  className?: string;
}

const ImageUploader: React.FC<ImageUploaderProps> = ({
  images,
  onImagesChange,
  maxImages = 10,
  maxFileSize = 5,
  acceptedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'],
  disabled = false,
  className,
}) => {
  const [uploadingImages, setUploadingImages] = useState<ProductImageUpload[]>([]);
  const [dragActive, setDragActive] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Validate file
  const validateFile = (file: File): string | null => {
    if (!acceptedTypes.includes(file.type)) {
      return `File type ${file.type} is not supported. Please use ${acceptedTypes.join(', ')}.`;
    }

    if (file.size > maxFileSize * 1024 * 1024) {
      return `File size must be less than ${maxFileSize}MB.`;
    }

    if (images.length + uploadingImages.length >= maxImages) {
      return `Maximum ${maxImages} images allowed.`;
    }

    return null;
  };

  // Create preview URL
  const createPreviewUrl = (file: File): string => {
    return URL.createObjectURL(file);
  };

  // Upload single file
  const uploadFile = async (uploadImage: ProductImageUpload): Promise<string> => {
    try {
      const url = await productService.uploadProductImages([uploadImage.file]);
      return url[0];
    } catch (error) {
      throw new Error(error instanceof Error ? error.message : 'Upload failed');
    }
  };

  // Handle file upload
  const handleFileUpload = useCallback(async (files: File[]) => {
    setError(null);
    
    const validFiles: ProductImageUpload[] = [];
    const errors: string[] = [];

    // Validate all files first
    for (const file of files) {
      const validationError = validateFile(file);
      if (validationError) {
        errors.push(`${file.name}: ${validationError}`);
      } else {
        validFiles.push({
          file,
          preview: createPreviewUrl(file),
          isUploading: true,
          uploadProgress: 0,
        });
      }
    }

    if (errors.length > 0) {
      setError(errors.join('\n'));
      return;
    }

    if (validFiles.length === 0) return;

    // Add to uploading state
    setUploadingImages(prev => [...prev, ...validFiles]);

    // Upload files
    const uploadPromises = validFiles.map(async (uploadImage, index) => {
      try {
        // Simulate upload progress
        const progressInterval = setInterval(() => {
          setUploadingImages(prev => 
            prev.map(img => 
              img.file === uploadImage.file 
                ? { ...img, uploadProgress: Math.min((img.uploadProgress || 0) + 10, 90) }
                : img
            )
          );
        }, 200);

        const uploadedUrl = await uploadFile(uploadImage);
        
        clearInterval(progressInterval);
        
        // Update progress to 100%
        setUploadingImages(prev => 
          prev.map(img => 
            img.file === uploadImage.file 
              ? { ...img, uploadProgress: 100, isUploading: false }
              : img
          )
        );

        return uploadedUrl;
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : 'Upload failed';
        
        // Update error state
        setUploadingImages(prev => 
          prev.map(img => 
            img.file === uploadImage.file 
              ? { ...img, isUploading: false, error: errorMessage }
              : img
          )
        );
        
        return null;
      }
    });

    try {
      const results = await Promise.all(uploadPromises);
      const successfulUploads = results.filter((url): url is string => url !== null);
      
      // Update images list
      onImagesChange([...images, ...successfulUploads]);
      
      // Clean up uploading state (keep only failed uploads)
      setUploadingImages(prev => prev.filter(img => img.error));
    } catch (error) {
      setError('Some uploads failed. Please try again.');
    }
  }, [images, onImagesChange, maxImages, maxFileSize, acceptedTypes]);

  // Handle drag events
  const handleDrag = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  }, []);

  // Handle drop
  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    if (disabled) return;

    const files = Array.from(e.dataTransfer.files);
    handleFileUpload(files);
  }, [handleFileUpload, disabled]);

  // Handle file input change
  const handleFileInputChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files || disabled) return;
    
    const files = Array.from(e.target.files);
    handleFileUpload(files);
    
    // Clear input
    e.target.value = '';
  }, [handleFileUpload, disabled]);

  // Remove image
  const removeImage = useCallback((index: number) => {
    const newImages = images.filter((_, i) => i !== index);
    onImagesChange(newImages);
  }, [images, onImagesChange]);

  // Remove uploading image
  const removeUploadingImage = useCallback((fileToRemove: File) => {
    setUploadingImages(prev => {
      const imageToRemove = prev.find(img => img.file === fileToRemove);
      if (imageToRemove) {
        URL.revokeObjectURL(imageToRemove.preview);
      }
      return prev.filter(img => img.file !== fileToRemove);
    });
  }, []);

  // Retry failed upload
  const retryUpload = useCallback((fileToRetry: File) => {
    const uploadImage = uploadingImages.find(img => img.file === fileToRetry);
    if (uploadImage) {
      handleFileUpload([fileToRetry]);
    }
  }, [uploadingImages, handleFileUpload]);

  // Click to upload
  const handleUploadClick = () => {
    fileInputRef.current?.click();
  };

  const canUpload = !disabled && (images.length + uploadingImages.length) < maxImages;

  return (
    <div className={cn('space-y-4', className)}>
      {/* Upload Area */}
      {canUpload && (
        <div
          className={cn(
            'border-2 border-dashed rounded-lg p-6 text-center transition-colors',
            dragActive 
              ? 'border-ocean-500 bg-ocean-50' 
              : 'border-gray-300 hover:border-gray-400',
            disabled && 'opacity-50 cursor-not-allowed'
          )}
          onDragEnter={handleDrag}
          onDragLeave={handleDrag}
          onDragOver={handleDrag}
          onDrop={handleDrop}
        >
          <PhotoIcon className="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">
            Upload Product Images
          </h3>
          <p className="text-sm text-gray-600 mb-4">
            Drag and drop images here, or click to browse
          </p>
          <p className="text-xs text-gray-500 mb-4">
            Supports: {acceptedTypes.join(', ')} • Max {maxFileSize}MB each • Up to {maxImages} images
          </p>
          
          <Button 
            variant="outline" 
            onClick={handleUploadClick}
            disabled={disabled}
            leftIcon={<ArrowUpTrayIcon className="h-4 w-4" />}
          >
            Browse Files
          </Button>
          
          <input
            ref={fileInputRef}
            type="file"
            multiple
            accept={acceptedTypes.join(',')}
            onChange={handleFileInputChange}
            className="hidden"
            disabled={disabled}
          />
        </div>
      )}

      {/* Error Message */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-3">
          <div className="flex items-center">
            <ExclamationTriangleIcon className="h-5 w-5 text-red-500 mr-2" />
            <div className="text-sm text-red-700 whitespace-pre-line">
              {error}
            </div>
          </div>
        </div>
      )}

      {/* Uploading Images */}
      {uploadingImages.length > 0 && (
        <div className="space-y-3">
          <h4 className="text-sm font-medium text-gray-700">Uploading...</h4>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
            {uploadingImages.map((uploadImage, index) => (
              <div key={index} className="relative group">
                <div className="aspect-square bg-gray-100 rounded-lg overflow-hidden">
                  <img
                    src={uploadImage.preview}
                    alt="Uploading"
                    className="w-full h-full object-cover"
                  />
                  
                  {/* Overlay */}
                  <div className="absolute inset-0 bg-black bg-opacity-50 flex items-center justify-center">
                    {uploadImage.isUploading ? (
                      <div className="text-center text-white">
                        <div className="mb-2">
                          <div className="w-8 h-8 border-2 border-white border-t-transparent rounded-full animate-spin mx-auto"></div>
                        </div>
                        <p className="text-xs">
                          {uploadImage.uploadProgress}%
                        </p>
                      </div>
                    ) : uploadImage.error ? (
                      <div className="text-center text-white">
                        <ExclamationTriangleIcon className="h-6 w-6 mx-auto mb-2" />
                        <p className="text-xs">Failed</p>
                      </div>
                    ) : (
                      <div className="text-center text-white">
                        <div className="w-6 h-6 bg-green-500 rounded-full flex items-center justify-center mx-auto mb-1">
                          ✓
                        </div>
                        <p className="text-xs">Done</p>
                      </div>
                    )}
                  </div>

                  {/* Remove Button */}
                  <button
                    onClick={() => removeUploadingImage(uploadImage.file)}
                    className="absolute top-1 right-1 bg-red-500 text-white rounded-full p-1 opacity-0 group-hover:opacity-100 transition-opacity"
                  >
                    <XMarkIcon className="h-3 w-3" />
                  </button>

                  {/* Retry Button (for failed uploads) */}
                  {uploadImage.error && (
                    <button
                      onClick={() => retryUpload(uploadImage.file)}
                      className="absolute bottom-1 right-1 bg-blue-500 text-white rounded px-2 py-1 text-xs opacity-0 group-hover:opacity-100 transition-opacity"
                    >
                      Retry
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Uploaded Images */}
      {images.length > 0 && (
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h4 className="text-sm font-medium text-gray-700">
              Product Images ({images.length}/{maxImages})
            </h4>
          </div>
          
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
            {images.map((image, index) => (
              <div key={index} className="relative group">
                <div className="aspect-square bg-gray-100 rounded-lg overflow-hidden">
                  <img
                    src={image}
                    alt={`Product image ${index + 1}`}
                    className="w-full h-full object-cover"
                  />
                  
                  {/* Primary Badge */}
                  {index === 0 && (
                    <div className="absolute top-2 left-2 bg-ocean-500 text-white text-xs px-2 py-1 rounded">
                      Primary
                    </div>
                  )}

                  {/* Remove Button */}
                  <button
                    onClick={() => removeImage(index)}
                    disabled={disabled}
                    className="absolute top-1 right-1 bg-red-500 text-white rounded-full p-1 opacity-0 group-hover:opacity-100 transition-opacity disabled:cursor-not-allowed"
                  >
                    <XMarkIcon className="h-3 w-3" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Empty State */}
      {images.length === 0 && uploadingImages.length === 0 && !canUpload && (
        <div className="text-center py-6 text-gray-500">
          <PhotoIcon className="h-12 w-12 mx-auto mb-2 text-gray-300" />
          <p className="text-sm">No images uploaded yet</p>
        </div>
      )}
    </div>
  );
};

export default ImageUploader;