import React, { useState } from 'react';
import { Form, Button } from 'react-bootstrap';
import StarRating from './StarRating';
import { reviewService } from '../services/reviewService';
import { toast } from 'react-toastify';

interface ReviewFormProps {
    goodsId: string;
    onReviewSubmitted: () => void;
}

const ReviewForm: React.FC<ReviewFormProps> = ({ goodsId, onReviewSubmitted }) => {
    const [rating, setRating] = useState(0);
    const [comment, setComment] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [commentError, setCommentError] = useState<string>('');
    const [ratingError, setRatingError] = useState<string>('');

    const validateComment = (value: string): string => {
        const trimmed = value.trim();
        if (!trimmed) return 'Комментарий обязателен';
        if (trimmed.length < 10) return 'Минимум 10 символов';
        if (trimmed.length > 1000) return 'Максимум 1000 символов';
        return '';
    };

    const handleCommentChange = (value: string) => {
        setComment(value);
        const error = validateComment(value);
        setCommentError(error);
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (rating < 1) {
            setRatingError('Пожалуйста, выберите оценку');
            return;
        }

        const error = validateComment(comment);
        if (error) {
            setCommentError(error);
            return;
        }

        setSubmitting(true);
        try {
            await reviewService.createReview({ goodsId, rating, comment: comment.trim() });
            toast.success('Отзыв успешно добавлен!');
            setRating(0);
            setComment('');
            setCommentError('');
            setRatingError('');
            onReviewSubmitted();
        } catch (error: any) {
            toast.error(error.response?.data?.message || 'Ошибка при добавлении отзыва');
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="review-form">
            <h4>Оставить отзыв</h4>
            <Form onSubmit={handleSubmit}>
                <Form.Group className="mb-3">
                    <Form.Label>Ваша оценка</Form.Label>
                    <div>
                        <StarRating
                            rating={rating}
                            readonly={false}
                            onRatingChange={(value) => { setRating(value); setRatingError(''); }}
                        />
                    </div>
                    {ratingError
                        ? <div className="text-danger small mt-1">{ratingError}</div>
                        : <Form.Text className="text-muted">Нажмите на звёзды, чтобы поставить оценку</Form.Text>
                    }
                </Form.Group>

                <Form.Group className="mb-3">
                    <Form.Label>Ваш отзыв</Form.Label>
                    <Form.Control
                        as="textarea"
                        rows={4}
                        value={comment}
                        onChange={(e) => handleCommentChange(e.target.value)}
                        onBlur={() => {
                            const error = validateComment(comment);
                            setCommentError(error);
                        }}
                        placeholder="Поделитесь своими впечатлениями о товаре..."
                        isInvalid={!!commentError}
                        required
                        minLength={10}
                        maxLength={1000}
                    />
                    <Form.Control.Feedback type="invalid">
                        {commentError}
                    </Form.Control.Feedback>
                    <Form.Text className="text-muted">
                        {comment.length}/1000 символов (минимум 10)
                    </Form.Text>
                </Form.Group>

                <Button
                    type="submit"
                    variant="success"
                    disabled={submitting || !!commentError || comment.trim().length < 10}
                >
                    {submitting ? 'Отправка...' : 'Отправить отзыв'}
                </Button>
            </Form>
        </div>
    );
};

export default ReviewForm;
