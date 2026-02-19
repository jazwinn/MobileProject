from bing_image_downloader import downloader
import os


def scrape_images():
    print("--- Image Scraper ---")

    # 1. Get input from the user
    query = input("What do you want to search for? \n> ")

    try:
        limit = int(input("How many images do you want to download? \n> "))
    except ValueError:
        print("Please enter a valid number.")
        return

    print(f"\nSearching for '{query}' and downloading up to {limit} images...")

    # 2. Define where the images will be saved
    # This creates a folder in the same directory as your script
    output_directory = 'scraped_images'

    # 3. Run the downloader
    try:
        downloader.download(
            query,
            limit=limit,
            output_dir=output_directory,
            adult_filter_off=False,  # Set to True if you want to disable the safe search filter
            force_replace=False,  # Set to True to overwrite existing files with the same name
            timeout=60,  # How long to wait for an image to download before giving up
            verbose=True  # Prints the progress to your console
        )
        print(f"\nSuccess! Your images have been saved to the '{output_directory}/{query}' folder.")

    except Exception as e:
        print(f"\nAn error occurred: {e}")


if __name__ == "__main__":
    scrape_images()