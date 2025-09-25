/**
 * Prepends the necessary data URI scheme to a base64 string if it's missing.
 * This function can detect common image formats like JPEG, PNG, and GIF based on their magic numbers.
 * @param {string | null | undefined} base64String - The base64 encoded image string.
 * @returns {string} - The formatted data URI, or an empty string if the input is invalid.
 */
export function formatBase64Image(base64String: string | null | undefined): string {
  if (!base64String) {
    return ''; // Return an empty string for invalid input
  }

  // If it's already a data URI, return it as is.
  if (base64String.startsWith('data:image')) {
    return base64String;
  }

  // Detect image format from base64 magic numbers
  let mimeType: string;

  // Check the first few characters of the base64 string
  if (base64String.startsWith('/9j/')) {
    mimeType = 'image/jpeg';
  } else if (base64String.startsWith('iVBOR')) {
    mimeType = 'image/png';
  } else if (base64String.startsWith('R0lGOD')) {
    mimeType = 'image/gif';
  } else if (base64String.startsWith('UklGR')) {
    mimeType = 'image/webp';
  } else if (base64String.startsWith('PHN2Zy')) {
    mimeType = 'image/svg+xml';
  } else {
    // As a fallback, assume it's a PNG, which is a common case.
    // A more robust solution might involve returning an empty string or a default placeholder image.
    mimeType = 'image/png';
  }

  return `data:${mimeType};base64,${base64String}`;
}