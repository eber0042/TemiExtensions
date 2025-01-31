import qrcode
from PIL import Image, ImageDraw, ImageFont

# Data for QR code
data = "https://forms.gle/jwaodpfWMuvDUn526"

# Create QR code
qr = qrcode.QRCode(
    version=1,
    error_correction=qrcode.constants.ERROR_CORRECT_L,
    box_size=10,
    border=4,
)
qr.add_data(data)
qr.make(fit=True)

# Create QR code image
img = qr.make_image(fill_color="black", back_color="white")

# Ensure the image is in RGB mode
img = img.convert("RGB")

# Add text below QR code
text = "Scan me to leave feedback!"

# Load a custom font and set a larger size (e.g., 40)
font_size = 25
font_path = "C:/Windows/Fonts/arial.ttf"  # You can change this path to another font file if desired
font = ImageFont.truetype(font_path, font_size)

# Create a new image with extra space for the text
width, height = img.size
new_height = height + font_size + 10  # Add space for text
new_img = Image.new("RGB", (width, new_height), "white")

# Paste the QR code onto the new image
new_img.paste(img, (0, 0))

# Draw text on the new image
draw = ImageDraw.Draw(new_img)

# Calculate text size using textbbox
text_bbox = draw.textbbox((0, 0), text, font=font)
text_width = text_bbox[2] - text_bbox[0]
text_height = text_bbox[3] - text_bbox[1]

# Position the text
text_x = (width - text_width) // 2
text_y = height + 5
draw.text((text_x, text_y), text, fill="black", font=font)

# Save the new image
new_img.save("custom_qrcode.png")
print("QR code with text saved as 'custom_qrcode.png'")