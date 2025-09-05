import React, { useState, useEffect } from 'react';
import { apiClient } from '../../services/api';
import LoadingSpinner from '../ui/LoadingSpinner';
import Button from '../ui/Button';
import Card from '../ui/Card';
import { cn } from '../../utils/cn';

// Icons
const StarIcon = ({ filled = false, className = "" }: { filled?: boolean; className?: string }) => (
  <svg className={cn("w-4 h-4", className)} fill={filled ? "currentColor" : "none"} stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
  </svg>
);

const ThumbsUpIcon = ({ className = "" }: { className?: string }) => (
  <svg className={cn("w-4 h-4", className)} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 10h4.764a2 2 0 011.789 2.894l-3.5 7A2 2 0 0115.263 21h-4.017c-.163 0-.326-.02-.485-.06L7 20m7-10V5a2 2 0 00-2-2v0a2 2 0 00-2 2v4.5m0 0L7 20m0 0v-4.5m0 4.5H3" />
  </svg>
);

const ThumbsDownIcon = ({ className = "" }: { className?: string }) => (
  <svg className={cn("w-4 h-4", className)} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 14H5.236a2 2 0 01-1.789-2.894l3.5-7A2 2 0 018.736 3h4.018c.163 0 .326.02.485.06L17 4m-7 10v2a2 2 0 002 2v0a2 2 0 002-2v-6.5M10 14l4-4m-4 4H3" />
  </svg>
);

const UserIcon = ({ className = "" }: { className?: string }) => (
  <svg className={cn("w-5 h-5", className)} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
  </svg>
);

interface Review {
  id: string;
  userId: string;
  userName: string;
  userAvatar?: string;
  rating: number;
  title: string;
  comment: string;
  createdAt: string;
  helpfulCount: number;
  verified: boolean;
  images?: string[];
}

interface ReviewSummary {
  averageRating: number;
  totalReviews: number;
  ratingDistribution: Record<number, number>;
}

interface ProductReviewsProps {
  productId: string;
  className?: string;
}

const ProductReviews: React.FC<ProductReviewsProps> = ({ productId, className }) => {
  const [reviews, setReviews] = useState<Review[]>([]);
  const [reviewSummary, setReviewSummary] = useState<ReviewSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const [sortBy, setSortBy] = useState<'newest' | 'oldest' | 'rating_high' | 'rating_low' | 'helpful'>('newest');
  const [filterRating, setFilterRating] = useState<number | null>(null);

  // Load reviews
  useEffect(() => {
    loadReviews(true);
  }, [productId, sortBy, filterRating]);

  const loadReviews = async (reset: boolean = false) => {
    if (reset) {
      setIsLoading(true);
      setPage(1);
    }

    try {
      const params = {
        page: reset ? 1 : page,
        limit: 10,
        sortBy,
        ...(filterRating && { rating: filterRating }),
      };

      const [reviewsResponse, summaryResponse] = await Promise.all([
        apiClient.request<{ data: Review[]; hasMore: boolean }>({
          method: 'GET',
          url: `/products/${productId}/reviews`,
          params,
        }),
        apiClient.request<ReviewSummary>({
          method: 'GET',
          url: `/products/${productId}/reviews/summary`,
        }),
      ]);

      if (reset) {
        setReviews(reviewsResponse.data);
      } else {
        setReviews(prev => [...prev, ...reviewsResponse.data]);
      }

      setReviewSummary(summaryResponse);
      setHasMore(reviewsResponse.hasMore);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load reviews');
    } finally {
      setIsLoading(false);
    }
  };

  // Handle load more
  const handleLoadMore = () => {
    setPage(prev => prev + 1);
    loadReviews(false);
  };

  // Handle helpful vote
  const handleHelpfulVote = async (reviewId: string, isHelpful: boolean) => {
    try {
      await apiClient.request({
        method: 'POST',
        url: `/reviews/${reviewId}/vote`,
        data: { helpful: isHelpful },
      });

      // Update local state
      setReviews(prev => prev.map(review => 
        review.id === reviewId 
          ? { ...review, helpfulCount: review.helpfulCount + (isHelpful ? 1 : -1) }
          : review
      ));
    } catch (error) {
      console.error('Failed to vote on review:', error);
    }
  };

  // Render star rating
  const renderStarRating = (rating: number, size: 'sm' | 'md' = 'sm') => {
    const stars = [];
    const sizeClass = size === 'sm' ? 'w-4 h-4' : 'w-5 h-5';
    
    for (let i = 1; i <= 5; i++) {
      stars.push(
        <StarIcon
          key={i}
          filled={i <= rating}
          className={cn(
            sizeClass,
            i <= rating ? "text-yellow-400" : "text-gray-300"
          )}
        />
      );
    }
    return stars;
  };

  // Format date
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  // Loading state
  if (isLoading && reviews.length === 0) {
    return (
      <div className={cn("flex justify-center py-8", className)}>
        <LoadingSpinner size="md" />
      </div>
    );
  }

  // Error state
  if (error && reviews.length === 0) {
    return (
      <div className={cn("text-center py-8 text-gray-500", className)}>
        <p>Failed to load reviews: {error}</p>
        <Button onClick={() => loadReviews(true)} variant="outline" size="sm" className="mt-2">
          Retry
        </Button>
      </div>
    );
  }

  return (
    <div className={cn("space-y-6", className)}>
      {/* Review Summary */}
      {reviewSummary && (
        <Card className="p-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Overall Rating */}
            <div className="text-center md:text-left">
              <div className="flex flex-col md:flex-row md:items-center md:space-x-4">
                <div className="text-4xl font-bold text-gray-900 mb-2 md:mb-0">
                  {reviewSummary.averageRating.toFixed(1)}
                </div>
                <div>
                  <div className="flex justify-center md:justify-start mb-1">
                    {renderStarRating(Math.round(reviewSummary.averageRating), 'md')}
                  </div>
                  <p className="text-sm text-gray-600">
                    Based on {reviewSummary.totalReviews} review{reviewSummary.totalReviews !== 1 ? 's' : ''}
                  </p>
                </div>
              </div>
            </div>

            {/* Rating Distribution */}
            <div className="space-y-2">
              {[5, 4, 3, 2, 1].map((rating) => {
                const count = reviewSummary.ratingDistribution[rating] || 0;
                const percentage = reviewSummary.totalReviews > 0 
                  ? (count / reviewSummary.totalReviews) * 100 
                  : 0;

                return (
                  <div key={rating} className="flex items-center space-x-2">
                    <span className="text-sm text-gray-600 w-8">{rating}★</span>
                    <div className="flex-1 bg-gray-200 rounded-full h-2">
                      <div
                        className="bg-yellow-400 h-2 rounded-full transition-all duration-300"
                        style={{ width: `${percentage}%` }}
                      />
                    </div>
                    <span className="text-sm text-gray-600 w-8">{count}</span>
                  </div>
                );
              })}
            </div>
          </div>
        </Card>
      )}

      {/* Filters and Sort */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="flex items-center space-x-4">
          {/* Rating Filter */}
          <div className="flex items-center space-x-2">
            <label className="text-sm font-medium text-gray-700">Filter:</label>
            <select
              value={filterRating || ''}
              onChange={(e) => setFilterRating(e.target.value ? parseInt(e.target.value) : null)}
              className="text-sm border border-gray-300 rounded px-2 py-1 focus:ring-2 focus:ring-ocean-500 focus:border-ocean-500"
            >
              <option value="">All Ratings</option>
              <option value="5">5 Stars</option>
              <option value="4">4 Stars</option>
              <option value="3">3 Stars</option>
              <option value="2">2 Stars</option>
              <option value="1">1 Star</option>
            </select>
          </div>
        </div>

        {/* Sort Options */}
        <div className="flex items-center space-x-2">
          <label className="text-sm font-medium text-gray-700">Sort:</label>
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as typeof sortBy)}
            className="text-sm border border-gray-300 rounded px-2 py-1 focus:ring-2 focus:ring-ocean-500 focus:border-ocean-500"
          >
            <option value="newest">Newest First</option>
            <option value="oldest">Oldest First</option>
            <option value="rating_high">Highest Rating</option>
            <option value="rating_low">Lowest Rating</option>
            <option value="helpful">Most Helpful</option>
          </select>
        </div>
      </div>

      {/* Reviews List */}
      {reviews.length === 0 ? (
        <div className="text-center py-8 text-gray-500">
          <p>No reviews yet. Be the first to review this product!</p>
        </div>
      ) : (
        <div className="space-y-4">
          {reviews.map((review) => (
            <Card key={review.id} className="p-6">
              <div className="space-y-4">
                {/* Review Header */}
                <div className="flex items-start justify-between">
                  <div className="flex items-start space-x-3">
                    {/* User Avatar */}
                    <div className="flex-shrink-0">
                      {review.userAvatar ? (
                        <img
                          src={review.userAvatar}
                          alt={review.userName}
                          className="w-10 h-10 rounded-full object-cover"
                        />
                      ) : (
                        <div className="w-10 h-10 bg-gray-200 rounded-full flex items-center justify-center">
                          <UserIcon className="w-5 h-5 text-gray-400" />
                        </div>
                      )}
                    </div>

                    {/* User Info */}
                    <div>
                      <div className="flex items-center space-x-2">
                        <p className="font-medium text-gray-900">{review.userName}</p>
                        {review.verified && (
                          <span className="bg-green-100 text-green-800 text-xs px-2 py-0.5 rounded-full">
                            Verified Purchase
                          </span>
                        )}
                      </div>
                      <div className="flex items-center space-x-2 mt-1">
                        <div className="flex">{renderStarRating(review.rating)}</div>
                        <span className="text-sm text-gray-500">{formatDate(review.createdAt)}</span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Review Content */}
                <div>
                  {review.title && (
                    <h4 className="font-medium text-gray-900 mb-2">{review.title}</h4>
                  )}
                  <p className="text-gray-700 leading-relaxed">{review.comment}</p>
                </div>

                {/* Review Images */}
                {review.images && review.images.length > 0 && (
                  <div className="grid grid-cols-4 gap-2">
                    {review.images.slice(0, 4).map((image, index) => (
                      <div key={index} className="aspect-square bg-gray-100 rounded overflow-hidden">
                        <img
                          src={image}
                          alt={`Review ${index + 1}`}
                          className="w-full h-full object-cover"
                        />
                      </div>
                    ))}
                  </div>
                )}

                {/* Review Actions */}
                <div className="flex items-center justify-between pt-4 border-t border-gray-100">
                  <div className="flex items-center space-x-4">
                    <button
                      onClick={() => handleHelpfulVote(review.id, true)}
                      className="flex items-center space-x-1 text-sm text-gray-600 hover:text-green-600 transition-colors duration-200"
                    >
                      <ThumbsUpIcon />
                      <span>Helpful ({review.helpfulCount})</span>
                    </button>
                    
                    <button
                      onClick={() => handleHelpfulVote(review.id, false)}
                      className="flex items-center space-x-1 text-sm text-gray-600 hover:text-red-600 transition-colors duration-200"
                    >
                      <ThumbsDownIcon />
                      <span>Not Helpful</span>
                    </button>
                  </div>

                  <button className="text-sm text-gray-600 hover:text-gray-800 transition-colors duration-200">
                    Report
                  </button>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* Load More Button */}
      {hasMore && !isLoading && (
        <div className="text-center">
          <Button onClick={handleLoadMore} variant="outline" size="lg">
            Load More Reviews
          </Button>
        </div>
      )}

      {/* Loading More Indicator */}
      {isLoading && reviews.length > 0 && (
        <div className="text-center py-4">
          <LoadingSpinner size="sm" />
        </div>
      )}
    </div>
  );
};

export default ProductReviews;