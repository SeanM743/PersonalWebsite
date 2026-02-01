import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import LifeLogEntryForm from './LifeLogEntryForm';
import { LifeLogType, EntryStatus } from './LifeLogView';
import { NotificationProvider } from '../contexts/NotificationContext';

// Mock dependencies
jest.mock('../services/apiService', () => ({
    apiService: {
        createLifeLogEntry: jest.fn(),
        updateLifeLogEntry: jest.fn(),
        searchMetadata: jest.fn()
    }
}));

const renderWithContext = (component: React.ReactNode) => {
    return render(
        <NotificationProvider>
            {component}
        </NotificationProvider>
    );
};

describe('LifeLogEntryForm Date Logic', () => {
    const mockClose = jest.fn();
    const mockSave = jest.fn();

    beforeEach(() => {
        jest.clearAllMocks();
    });

    test('shows Start/End Date for BOOK type', () => {
        renderWithContext(
            <LifeLogEntryForm
                isOpen={true}
                onClose={mockClose}
                onSave={mockSave}
            />
        );

        // Initial state is BOOK
        expect(screen.getByLabelText(/Start Date/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/End Date/i)).toBeInTheDocument();
        expect(screen.queryByLabelText(/Date Watched/i)).not.toBeInTheDocument();
    });

    test('shows Date Watched for MOVIE type', async () => {
        renderWithContext(
            <LifeLogEntryForm
                isOpen={true}
                onClose={mockClose}
                onSave={mockSave}
            />
        );

        // Change type to MOVIE
        const typeSelect = screen.getByLabelText(/Type \*/i);
        fireEvent.change(typeSelect, { target: { value: LifeLogType.MOVIE } });

        // Expect "Date Watched" to appear
        expect(screen.getByLabelText(/Date Watched/i)).toBeInTheDocument();

        // Expect "Start Date" to disappear
        expect(screen.queryByLabelText(/Start Date/i)).not.toBeInTheDocument();

        // "End Date" label is gone (replaced by Date Watched)
        // We check specifically that we don't see the text "End Date" as a label
        expect(screen.queryByText('End Date')).not.toBeInTheDocument();
    });

    test('image preview appears when URL is set', () => {
        renderWithContext(
            <LifeLogEntryForm
                isOpen={true}
                onClose={mockClose}
                onSave={mockSave}
            />
        );

        const urlInput = screen.getByPlaceholderText('https://example.com/image.jpg');
        fireEvent.change(urlInput, { target: { value: 'https://test.com/img.jpg' } });

        const previewImg = screen.getByAltText('Preview');
        expect(previewImg).toBeInTheDocument();
        expect(previewImg).toHaveAttribute('src', 'https://test.com/img.jpg');
    });
});
