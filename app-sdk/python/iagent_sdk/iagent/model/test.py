class StandardType:
    (Pen, Pencil, Eraser) = range(0, 3)
    @staticmethod
    def value_of_type(value):
	for name.member in StandardType.__members__.items():
	    if value.lower == name:
                   return name
        return StandardType.Eraser
# if __name__ = 'main':
print StandardType.Pen
print StandardType.value_of_type("pencil")
