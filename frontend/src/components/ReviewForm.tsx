import React, { useState } from 'react';
import { Form, Button } from 'react-bootstrap';
import StarRating from './StarRating';
import { reviewService } from '../services/reviewService';
import { toast } from 'react-toastify';

interface ReviewFormProps {
    plantId: string;
    onReviewSubmitted: () => void;
}

const ReviewForm: React.FC<ReviewFormProps> = ({ plantId, onReviewSubmitted }) => {
    const [rating, setRating] = useState(5);
    const [comment, setComment] = useState('');
    const [submitting, setSubmitting] = useState(false);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (comment.trim().length < 10) {
            toast.error('Отзыв должен содержать минимум 10 символов');
            return;
        }

        setSubmitting(true);
        try {
            await reviewService.createReview({ plantId, rating, comment: comment.trim() });
            toast.success('Отзыв успешно добавлен!');
            setRating(5);
            setComment('');
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
                            onRatingChange={setRating} 
                        />
                    </div>
                </Form.Group>

                <Form.Group className="mb-3">
                    <Form.Label>Ваш отзыв</Form.Label>
                    <Form.Control
                        as="textarea"
                        rows={4}
                        value={comment}
                        onChange={(e) => setComment(e.target.value)}
                        placeholder="Поделитесь своими впечатлениями о товаре..."
                        required
                        minLength={10}
                    />
                    <Form.Text className="text-muted">
                        Минимум 10 символов
                    </Form.Text>
                </Form.Group>

                <Button 
                    type="submit" 
                    variant="success" 
                    disabled={submitting || comment.trim().length < 10}
                >
                    {submitting ? 'Отправка...' : 'Отправить отзыв'}
                </Button>
            </Form>
        </div>
    );
};

export default ReviewForm;
