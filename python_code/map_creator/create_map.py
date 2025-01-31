from PIL import Image
import ast

# Step 1: Read the file and extract the data
with open("library_map_data.txt", "r") as file:
    # Extract the data for the number of columns
    first_line = file.readline().strip()
    
    # Read the second line containing the pixel data as a string
    second_line = file.readline().strip()

# Step 2: Convert the string representation of the list into an actual list of integers
try:
    numbers_list = ast.literal_eval(second_line)
    
    # Ensure that the result is a valid list of integers
    if isinstance(numbers_list, list) and all(isinstance(i, int) for i in numbers_list):
        
        # Step 3: Define the color mapping for each unique integer
        color_map = {
            -1: (255, 255, 255),   # White
            100: (0, 0, 0),  # Black
            70: (70, 70, 70),   # Dark Gray
            0: (169, 169, 169)       # Light Gray
        }
        
        # Step 4: Get the number of columns from the first line of the file
        columns = int(first_line.split(":")[1].strip())  # Assuming first line format like "Columns: 25"
        
        # Step 5: Calculate image dimensions
        total_pixels = len(numbers_list)
        height = total_pixels // columns  # Number of rows is total pixels divided by the number of columns
        width = columns  # The width is the number of columns
        
        # Step 6: Create a new image with the appropriate dimensions (RGB mode)
        image = Image.new('RGB', (width, height))

        # Step 7: Load the pixel data to modify
        pixels = image.load()
        
        # Step 8: Iterate over the numbers_list and set the pixels' colors
        for i in range(height):
            for j in range(width):
                index = i * width + j  # Calculate the index in the flattened list
                value = numbers_list[index]
                # Set the pixel to the corresponding color based on the value in the color_map
                pixels[j, i] = color_map.get(value, (0, 0, 0))  # Default to black if not found
        
        # Step 9: Save the image as a PNG
        image.save("output_image.png")

        # Optionally, display the image
        image.show()
        
        print("Image saved as output_image.png")
        
    else:
        print("The second line does not contain a valid list of integers.")
except (ValueError, SyntaxError):
    print("Error: The second line is not in a valid format.")